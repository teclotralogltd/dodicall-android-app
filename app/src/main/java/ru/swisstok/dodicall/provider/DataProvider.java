/*
 *
 * Copyright (C) 2016, Telco Cloud Trading & Logistic Ltd
 *
 * This file is part of dodicall.
 * dodicall is free software : you can redistribute it and / or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * dodicall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dodicall.If not, see <http://www.gnu.org/licenses/>.
 */

package ru.swisstok.dodicall.provider;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import ru.swisstok.dodicall.DodicallApplication;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ActiveCallActivity;
import ru.swisstok.dodicall.activity.ExportKeyActivity;
import ru.swisstok.dodicall.activity.IncomingCallActivity;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.bl.CallHistoriesList;
import ru.swisstok.dodicall.manager.BusinessLogicCallback;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.service.RegistrationGcmService;
import ru.swisstok.dodicall.service.SyncService;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Logger;
import ru.swisstok.dodicall.util.NotificationsUtils;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BaseUserStatus;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.ContactModel;
import ru.uls_global.dodicall.ContactModelList;
import ru.uls_global.dodicall.ContactModelSet;
import ru.uls_global.dodicall.ContactPresenceStatusModel;
import ru.uls_global.dodicall.ContactSubscriptionState;
import ru.uls_global.dodicall.ContactSubscriptionStatus;
import ru.uls_global.dodicall.ContactsContactList;
import ru.uls_global.dodicall.ContactsContactModel;
import ru.uls_global.dodicall.ContactsContactSet;
import ru.uls_global.dodicall.ContactsContactType;
import ru.uls_global.dodicall.StringList;
import ru.uls_global.dodicall.StringSet;

public class DataProvider extends ContentProvider {

    private static final String TAG = "DataProvider";

    public static final String AUTHORITY = "ru.swisstok.provider";
    public static final String CONTACTS_TABLE_NAME = "ru_swisstok_contactModels";
    public static final String CHATS_TABLE_NAME = "chats";
    public static final String MESSAGES_TABLE_NAME = "messages";
    public static final String LIST_SEPARATOR = ";";
    public static final String FAVORITE_MARKER = "FAVORITE:";
    public static final String METHOD_START_PHONEBOOK_SYNC = "startPhonebookSync";

    public static final int MIN_DDC_QUERY_LENGTH = 3;

    public static final String ACTION_CHAT_MESSAGES_UPDATED = "ru.swisstok.action.ChatMessagesUpdated";
    public static final String ACTION_CHATS_UPDATED = "ru.swisstok.action.ChatsUpdated";
    public static final String ACTION_CHATS_RECREATED = "ru.swisstok.action.ChatsRecreated";
    public static final String ACTION_CHATS_DELETED = "ru.swisstok.action.ChatsDeleted";
    public static final String ACTION_HISTORY_UPDATED = "ru.swisstok.action.HistoryUpdated";
    public static final String ACTION_CONTACTS_UPDATED = "ru.swisstok.action.ContactsUpdated";
    public static final String ACTION_LOGOUT = "ru.swisstok.action.Logout";

    public static final String EXTRA_IDS = "ids";

    public static final String SELECTION_INVITE_AND_REVOKE_CHAT_MEMBERS = "InviteAndRevokeChatMembers";
    public static final String ACTION_BADGE_UPDATE = "ru.swisstok.action.BadgeUpdate";

    private ConcurrentHashMap<Integer, ContactModel> mContacts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ContactPresenceStatusModel> mContactStatuses = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ContactModel> mDdcContacts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Contact> mOtherContacts = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Chat> mChats;
    private Map<String, String> mIncompleteMessageMap = new TreeMap<>();
    private final Object mChatAccessLock = new Object();

    private Pair<NotificationsUtils.NotificationType, Object> mCurrentActiveScreen;

    public static final int PHONEBOOK_MAGIC_ID = 39837770;
    public static final int INVITE_MAGIC_ID = 98543598;
    public static final int FILTER_SUBSCRIPTIONS = 6;
    public static final String ID_MY = "My";
//    public static final int FILTER_SEARCH = 7;

    private static final String CALLBACK_MODEL_NAME_CHATS = "Chats";
    private static final String CALLBACK_MODEL_NAME_CHAT_MESSAGES = "ChatMessages";
    private static final String CALLBACK_MODEL_CALLS = "Calls";
    private static final String CALLBACK_MODEL_NETWORK_STATE_CHANGED = "NetworkStateChanged";
    private static final String CALLBACK_MODEL_HISTORY = "History";
    private static final String CALLBACK_MODEL_LOGOUT = "Logout";
    private static final String CALLBACK_MODEL_LOGGED_IN = "LoggedIn";
    private static final String CALLBACK_MODEL_KEY_GENERATED = "SecretKey";

    public static final class ContactsColumn {
        public static final String ID = "_ID";
        public static final String DODICALL_ID = "DODICALL_ID";
        public static final String PHONEBOOK_ID = "PHONEBOOK_ID";
        public static final String NATIVE_ID = "NATIVE_ID";
        public static final String XMPP_ID = "XMPP_ID";
        public static final String FIRSTNAME = "FIRSTNAME";
        public static final String LASTNAME = "LASTNAME";
        public static final String MIDDLENAME = "MIDDLENAME";
        public static final String AVATAR_PATH = "AVATAR_PATH";
        public static final String BLOCKED = "BLOCKED";
        public static final String WHITE = "WHITE";
        public static final String BASE_STATUS = "BASE_STATUS";
        public static final String EXTRA_STATUS = "EXTRA_STATUS";
        //incoming
        public static final String INVITE = "INVITE";
        //outgoing
        public static final String SUBSCRIPTION_REQUEST = "SUBSCRIPTION_REQUEST";
        public static final String NEW_INVITE = "NEW_INVITE";
        public static final String DIRECTORY = "DIRECTORY";
        public static final String I_AM = "I_AM";
        public static final String IS_MINE = "IS_MINE";
        public static final String DECLINED_REQUEST = "DECLINED_REQUEST";
        public static final String SIP_LIST = "SIP_LIST";
        public static final String PHONES_LIST = "PHONES_LIST";
        public static final String SUBSCRIPTION_STATE = "SUBSCRIPTION_STATE";
    }

    public static final String[] CONTACTS_COLUMN = {
            ContactsColumn.ID, ContactsColumn.DODICALL_ID, ContactsColumn.PHONEBOOK_ID,
            ContactsColumn.NATIVE_ID, ContactsColumn.XMPP_ID, ContactsColumn.FIRSTNAME,
            ContactsColumn.LASTNAME, ContactsColumn.MIDDLENAME, ContactsColumn.AVATAR_PATH, ContactsColumn.BLOCKED,
            ContactsColumn.WHITE, ContactsColumn.BASE_STATUS, ContactsColumn.EXTRA_STATUS,
            ContactsColumn.INVITE, ContactsColumn.SUBSCRIPTION_REQUEST, ContactsColumn.NEW_INVITE,
            ContactsColumn.DIRECTORY, ContactsColumn.I_AM, ContactsColumn.IS_MINE, ContactsColumn.DECLINED_REQUEST,
            ContactsColumn.SIP_LIST, ContactsColumn.PHONES_LIST, ContactsColumn.SUBSCRIPTION_STATE
    };

    private static final String[] CHAT_COLUMNS = new String[]{
            Chat.COLUMN_ID,
            Chat.COLUMN_CUSTOM_TITLE,
            Chat.COLUMN_ACTIVE,
            Chat.COLUMN_TOTAL_MESSAGES_COUNT,
            Chat.COLUMN_NEW_MESSAGES_COUNT,
            Chat.COLUMN_LAST_MODIFIED_DATE,
            Chat.COLUMN_IS_P2P,
            Chat.COLUMN_INCOMPLETE_MESSAGE
    };

    private static final String[] MESSAGE_COLUMNS = new String[]{
            ChatMessage.COLUMN_ID,
            ChatMessage.COLUMN_CHAT_ID,
            ChatMessage.COLUMN_SERVERED,
            ChatMessage.COLUMN_SEND_TIME,
            ChatMessage.COLUMN_READ,
            ChatMessage.COLUMN_EDITED,
            ChatMessage.COLUMN_CONTENT,
            ChatMessage.COLUMN_TYPE,
            ChatMessage.COLUMN_ROWNUM,
            ChatMessage.COLUMN_ENCRYPTED
    };

    private static final String[] NOTIFICATION_DATA_COLUMNS = new String[]{
            ChatNotificationData.COLUMN_TYPE,
            ChatNotificationData.COLUMN_CHAT_MESSAGE_ID
    };

    public static final class CurrentCallColumns {
        public static final String ID = "CALL_ID";
        public static final String DIRECTION = "DIRECTION";
        public static final String ENCRYPTION = "ENCRYPTION";
        public static final String STATE = "STATE";
        public static final String DURATION = "DURATION";
        public static final String IDENTITY = "IDENTITY";
        public static final String ADDRESS_TYPE = "ADDRESS_TYPE";
        public static final String CONTACT = "CONTACT";
    }

    public static final String[] CURRENT_CALL_COLUMNS = new String[]{
            CurrentCallColumns.ID, CurrentCallColumns.DIRECTION, CurrentCallColumns.ENCRYPTION,
            CurrentCallColumns.STATE, CurrentCallColumns.DURATION, CurrentCallColumns.IDENTITY,
            CurrentCallColumns.ADDRESS_TYPE, CurrentCallColumns.CONTACT
    };

    public static final class CurrentActivityColumns {
        public static final String ACTIVITY_ID = "ACTIVITY_ID";
        public static final String NOTIFICATION_UPDATE_NEEDED = "NOTIFICATION_UPDATE_NEEDED";
    }

    public static final String CONTACTS_PATH = CONTACTS_TABLE_NAME;
    public static final String CHATS_PATH = CHATS_TABLE_NAME;
    public static final String MESSAGES_PATH = MESSAGES_TABLE_NAME;
    public static final String ACTIVITY_PATH = "activity";

    private static final String DDC_SEARCH_PATH = "ddc_search";

    private static final String INVITES_COUNT_PATH = "invites_count";
    public static final String COLUMN_INVITES_COUNT = INVITES_COUNT_PATH;

    public static final Uri CONTACTS_URI = Uri.parse(String.format("content://%s/%s", AUTHORITY, CONTACTS_PATH));
    public static final Uri CONTACTS_DDC_SEARCH_URI = Uri.withAppendedPath(
            CONTACTS_URI, DDC_SEARCH_PATH
    );
    public static final Uri MY_URI = Uri.withAppendedPath(CONTACTS_URI, ID_MY);
    public static final Uri INVITES_COUNT_URI = Uri.withAppendedPath(
            CONTACTS_URI, INVITES_COUNT_PATH
    );
    public static final String CURRENT_CALL_PATH = "current_call";
    public static final Uri CURRENT_CALL_URI = Uri.parse(
            String.format("content://%s/%s", AUTHORITY, CURRENT_CALL_PATH)
    );

    public static Uri makeDropDataUri() {
        return Uri.parse(String.format("content://%s/drop_data", AUTHORITY));
    }

    public static Uri makeRetrieveNumberUri(String number) {
        return Uri.parse(String.format("content://%s/retrieve_number/%s", AUTHORITY, number));
    }

    public static Uri makeMeUri() {
        return Uri.parse(String.format("content://%s/me", AUTHORITY));
    }

    public static Uri makeStatusUri(String xmppId) {
        return Uri.parse(String.format("content://%s/status/%s", AUTHORITY, xmppId));
    }

    public static Uri makeChatsUri() {
        return Uri.parse(String.format("content://%s/%s", AUTHORITY, CHATS_PATH));
    }

    public static Uri makeChatUri(String chatId) {
        return Uri.parse(String.format("content://%s/%s/%s", AUTHORITY, CHATS_PATH, chatId));
    }

    public static Uri makeChatContactsUri(String chatId) {
        return Uri.parse(String.format("content://%s/%s/%s/contacts", AUTHORITY, CHATS_PATH, chatId));
    }

    public static Uri makeChatMessagesUri(String chatId) {
        return Uri.parse(String.format("content://%s/%s/%s/messages", AUTHORITY, CHATS_PATH, chatId));
    }

    public static Uri makeChatMessagesSetReadUri(String chatId, String untilMessageId) {
        return Uri.parse(String.format("content://%s/%s/%s/messages/read/%s", AUTHORITY, CHATS_PATH, chatId, untilMessageId));
    }

    public static Uri makeChatIncompleteMessageUpdateUri(String chatId) {
        return Uri.parse(String.format("content://%s/%s/%s/messages/incomplete", AUTHORITY, CHATS_PATH, chatId));
    }

    public static Uri makeChatMessagesLastUri(String chatId) {
        return Uri.parse(String.format("content://%s/%s/%s/messages/last", AUTHORITY, CHATS_PATH, chatId));
    }

    public static Uri makeMessageSenderUri(String chatMessageId) {
        return Uri.parse(String.format("content://%s/%s/%s/sender", AUTHORITY, MESSAGES_PATH, chatMessageId));
    }

    public static Uri makeMessageNotificationDataUri(String chatMessageId) {
        return Uri.parse(String.format("content://%s/%s/%s/notification_data", AUTHORITY, MESSAGES_PATH, chatMessageId));
    }

    public static Uri makeMessageNotificationDataContactsUri(String chatMessageId) {
        return Uri.parse(String.format("content://%s/%s/%s/notification_data/contacts", AUTHORITY, MESSAGES_PATH, chatMessageId));
    }

    public static Uri makeCurrentActivityUri(NotificationsUtils.NotificationType notificationType) {
        return Uri.parse(String.format("content://%s/%s/%s", AUTHORITY, ACTIVITY_PATH, notificationType));
    }

    public static final String CONTACTS_CONTENT_TYPE = String.format("vnd.android.cursor.dir/vnd.%s.%s", AUTHORITY, CONTACTS_PATH);
    public static final String CONTACTS_CONTENT_ITEM_TYPE = String.format("vnd.android.cursor.item/vnd.%s.%s", AUTHORITY, CONTACTS_PATH);

    private static final int BASE_CONTACTS_URI_TYPE = 1;
    private static final int ID_CONTACTS_URI_TYPE = 2;
    private static final int MY_CONTACT_URI_TYPE = 21;
    private static final int DDC_SEARCH_URI_TYPE = 3;
    private static final int DDC_SEARCH_ID_URI_TYPE = 4;
    private static final int INVITES_COUNT_TYPE = 5;
    private static final int RETRIEVE_NUMBER = 6;
    private static final int CURRENT_CALL_URI_TYPE = 7;

    private static final int CHATS_URI_TYPE = 30;
    private static final int CHAT_ID_URI_TYPE = 31;
    private static final int CHATS_CONTACTS_URI_TYPE = 32;
    private static final int CHATS_MESSAGES_LAST_URI_TYPE = 34;
    private static final int CHATS_MESSAGES_SET_READ = 35;
    private static final int CHAT_INCOMPLETE_MESSAGE_URI_TYPE = 36;

    private static final int CHAT_MESSAGES_SENDER_URI_TYPE = 40;
    private static final int CHAT_MESSAGES_NOTIFICATION_DATA_URI_TYPE = 41;
    private static final int CHAT_MESSAGES_NOTIFICATION_DATA_CONTACTS_URI_TYPE = 42;

    private static final int ACTIVITY_URI_TYPE = 50;

    private static final int ME_URI_TYPE = 10000;
    private static final int STATUS_URI_TYPE = 10001;
    private static final int DROP_DATA_URI_TYPE = 20000;
    private static final UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, CONTACTS_TABLE_NAME, BASE_CONTACTS_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/#", CONTACTS_TABLE_NAME), ID_CONTACTS_URI_TYPE);
        sUriMatcher.addURI(
                AUTHORITY,
                String.format("%s/%s", CONTACTS_TABLE_NAME, ID_MY),
                MY_CONTACT_URI_TYPE
        );
        sUriMatcher.addURI(
                AUTHORITY,
                String.format("%s/%s", CONTACTS_TABLE_NAME, DDC_SEARCH_PATH),
                DDC_SEARCH_URI_TYPE
        );
        sUriMatcher.addURI(
                AUTHORITY,
                String.format("%s/%s/*", CONTACTS_TABLE_NAME, DDC_SEARCH_PATH),
                DDC_SEARCH_ID_URI_TYPE
        );
        sUriMatcher.addURI(
                AUTHORITY, String.format("%s/%s/", CONTACTS_TABLE_NAME, INVITES_COUNT_PATH),
                INVITES_COUNT_TYPE
        );

        sUriMatcher.addURI(AUTHORITY, "retrieve_number/*", RETRIEVE_NUMBER);

        //Chats
        sUriMatcher.addURI(AUTHORITY, CHATS_TABLE_NAME, CHATS_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*", CHATS_TABLE_NAME), CHAT_ID_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/contacts", CHATS_TABLE_NAME), CHATS_CONTACTS_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/messages/last", CHATS_TABLE_NAME), CHATS_MESSAGES_LAST_URI_TYPE);

        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/messages/read/*", CHATS_TABLE_NAME), CHATS_MESSAGES_SET_READ);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/messages/incomplete", CHATS_TABLE_NAME), CHAT_INCOMPLETE_MESSAGE_URI_TYPE);

        //Chat messages
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/sender", MESSAGES_TABLE_NAME), CHAT_MESSAGES_SENDER_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/notification_data", MESSAGES_TABLE_NAME), CHAT_MESSAGES_NOTIFICATION_DATA_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, String.format("%s/*/notification_data/contacts", MESSAGES_TABLE_NAME), CHAT_MESSAGES_NOTIFICATION_DATA_CONTACTS_URI_TYPE);

        //Me
        sUriMatcher.addURI(AUTHORITY, "me", ME_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, "status/*", STATUS_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, CURRENT_CALL_PATH, CURRENT_CALL_URI_TYPE);

        sUriMatcher.addURI(AUTHORITY, String.format("%s/*", ACTIVITY_PATH), ACTIVITY_URI_TYPE);
        sUriMatcher.addURI(AUTHORITY, "/drop_data", DROP_DATA_URI_TYPE);
    }

    private final BusinessLogicCallback
            businessLogicCallback = new BusinessLogicCallback() {
        @Override
        public void run(String modelName, StringList entityIds) {
            super.run(modelName, entityIds);

            try {
                D.log(TAG, "[CallbackFunction.run] modelName: %s", modelName);

                Context context = getContext();
                if (context == null) {
                    throw new IllegalStateException("Context is null inside BL callback");
                }

                if (TextUtils.equals(modelName, CALLBACK_MODEL_KEY_GENERATED)) {
                    StorageUtils.storeChatKey(context, BusinessLogic.GetInstance().GetUserKeys().toCharArray(), BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
                    context.startActivity(new Intent(context, ExportKeyActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else if (TextUtils.equals(modelName, CALLBACK_MODEL_LOGGED_IN)) {
                    Intent intent = new Intent(context, RegistrationGcmService.class);
                    context.startService(intent);
                }/* else if (TextUtils.equals(modelName, SyncService.MODEL_CONTACTS)) {
                    prepareContacts();
                }*/ else if (TextUtils.equals(modelName, SyncService.CONTACTS_PRESENCE)) {//todo move to chat manager
                    ArrayList<String> _ids = new ArrayList<>();
                    for (int i = 0; i < entityIds.size(); i++) {
                        String _id = entityIds.get(i);
                        _ids.add(_id);
                    }
                    if (CollectionUtils.isNotEmpty(_ids)) {
                        if (!prepareChatsContainer()) {
                            synchronized (mChatAccessLock) {
                                for (Chat chat : BL.getChats()) {
                                    mChats.put(chat.getId(), chat);
                                }
                            }
                        }
                    }
                } else if (TextUtils.equals(modelName, SyncService.CONTACT_SUBSCRIPTIONS)) {
                    updateInviteNotification();
                    LocalBroadcast.sendBroadcast(context, new Intent(ACTION_HISTORY_UPDATED));
                } else if (TextUtils.equals(modelName, CALLBACK_MODEL_NAME_CHATS)) {
                    D.log(TAG, "[CallbackFunction.run] chats updated!!!!!!");


                    if (mChats != null) {
                        ArrayList<String> updatedIdsList = new ArrayList<>();
                        ArrayList<String> deletedIdsList = new ArrayList<>();

                        synchronized (mChatAccessLock) {

                            String[] ids = new String[(int) entityIds.size()];
                            TreeSet<String> ts = new TreeSet<>();

                            for (int i = 0; i < entityIds.size(); ++i) {
                                final String id = entityIds.get(i);
                                ids[i] = id;
                                ts.add(id);
                            }

                            ArrayList<Chat> chats = BL.getChats(ids);
                            if (!chats.isEmpty()) {
                                for (Chat chat : chats) {
                                    final String chatId = chat.getId();

                                    mChats.put(chatId, chat);
                                    extractChatContacts(chat);
                                    ts.remove(chatId);

                                    updatedIdsList.add(chatId);
                                }

                                for (String deletedChatId : ts) {
                                    mChats.remove(deletedChatId);
                                    deletedIdsList.add(deletedChatId);
                                }
                            }
                        }

                        if (CollectionUtils.isNotEmpty(updatedIdsList)) {
                            sendLocalBroadcastForIds(context, ACTION_CHATS_UPDATED, updatedIdsList);
                        }

                        if (CollectionUtils.isNotEmpty(deletedIdsList)) {
                            sendLocalBroadcastForIds(context, ACTION_CHATS_DELETED, deletedIdsList);
                        }

                        updateChatNotification(false, updatedIdsList.toArray(new String[updatedIdsList.size()]));
                    }
                } else if (TextUtils.equals(modelName, CALLBACK_MODEL_NAME_CHAT_MESSAGES)) {
                    D.log(TAG, "[CallbackFunction.run] chat messages updated!!!!!!");

                    String[] ids = new String[(int) entityIds.size()];
                    for (int i = 0; i < entityIds.size(); ++i) {
                        ids[i] = entityIds.get(i);
                    }

                    LocalBroadcast.sendBroadcast(context, new Intent(ACTION_CHAT_MESSAGES_UPDATED).putExtra(EXTRA_IDS, ids));
                } else if (TextUtils.equals(modelName, SyncService.OFFLINE)) {
                /*for (ContactModel contactModel : mContacts.values()) {
                }*/
                    D.log(TAG, "[CallbackFunction.run] PresenceOffline");
                    context.getContentResolver().notifyChange(MY_URI, null);
                } else if (TextUtils.equals(modelName, CALLBACK_MODEL_CALLS)) {
                    D.log(TAG, "[CallbackFunction.run] Calls");
                    List<Call> calls = BL.getCalls();
                    final AudioManager am = getAudioManager(getContext());
                    if (!calls.isEmpty()) {
                        final Call call = calls.get(0);
                        if (call.state == Call.STATE_RINGING) {
                            if (call.direction == Call.DIRECTION_INCOMING) {
                                D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; ringing; incoming");
                                switch (am.getRingerMode()) {
                                    case AudioManager.RINGER_MODE_NORMAL:
                                        am.setMode(AudioManager.MODE_RINGTONE);
                                        am.setSpeakerphoneOn(true);
                                        break;
                                    case AudioManager.RINGER_MODE_SILENT:
                                        break;
                                    case AudioManager.RINGER_MODE_VIBRATE:
                                        break;
                                }
                                if (shouldShowIncomingCallScreen(context)) {
                                    D.log(TAG, "[CallbackFunction.run][offline_call_dbg] shouldShowIncomingCall");
                                    IncomingCallActivity.start(context, call);
                                }
                                NotificationsUtils.createIncomingCallNotification(context, call, !DodicallApplication.isVisible(context));
                            } else if (call.direction == Call.DIRECTION_OUTGOING) {
                                //TODO: remove it
                                D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; ringing; outgoing");
                                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                                am.setSpeakerphoneOn(false);
                                NotificationsUtils.createOutgoingCallNotification(context, call);
                            }
                        } else if (call.state == Call.STATE_CONVERSATION) {
                            D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; conversation;");
                            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                            if (call.direction == Call.DIRECTION_INCOMING) {
                                am.setSpeakerphoneOn(false);
                            }
                            CallReceiver.ActiveCallReceiver.activeCall(context);
                            if (!isCurrentActivity(context, ActiveCallActivity.class)) {
                                ActiveCallActivity.start(context, call);
                                Preferences.get(context).edit().remove(Preferences.Fields.PREF_DIALPAD_LAST_VAL).apply();
                            } else {
                                ActiveCallActivity.updateCall(context, call);
                            }
                            NotificationsUtils.createActiveCallNotification(context, call);
                        } else if (call.state == Call.STATE_DIALING) {
                            D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; dialing;");
                            if (call.direction == Call.DIRECTION_OUTGOING) {
                                D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; dialing; outgoing");
                                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                                am.setSpeakerphoneOn(false);
                                NotificationsUtils.createOutgoingCallNotification(context, call);
                            }
                        }
                    } else {
                        D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls empty; end");
                        if (am.isMicrophoneMute()) {
                            am.setMicrophoneMute(false);
                        }
//                    am.setSpeakerphoneOn(true);
                        am.setMode(AudioManager.MODE_NORMAL);
                        CallReceiver.EndCallReceiver.endCall(context);
                        NotificationsUtils.cancelNotification(context, NotificationsUtils.NotificationType.Call);
                    }
                    context.getContentResolver().notifyChange(CURRENT_CALL_URI, null);
                    return;
                } else if (TextUtils.equals(CALLBACK_MODEL_NETWORK_STATE_CHANGED, modelName)) {
                    context.sendBroadcast(new Intent(MainReceiver.MANUAL_NETWORK_CHANGE));
                    return;
                } else if (TextUtils.equals(CALLBACK_MODEL_HISTORY, modelName)) {
                    updateHistoryContacts();
                    LocalBroadcast.sendBroadcast(context, new Intent(ACTION_HISTORY_UPDATED));
                    updateHistoryNotification();
                    return;
                } else if (TextUtils.equals(CALLBACK_MODEL_LOGOUT, modelName)) {
                    LocalBroadcast.sendBroadcast(context, new Intent(ACTION_LOGOUT));
                    return;
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    AccountManager accountManager = AccountManager.get(context);
                    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.ACCOUNT_TYPE));
                    if (accounts.length > 0) {
                        SyncService.requestSyncManually(accounts[0], modelName);
                    }
                }

                updateBadge(context);

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    };

    public static void updateBadge(Context context) {
        int badgeCount =
                ContactsManagerImpl.getInstance().getNewInvites().size() +
                        BL.getAllMissedCalls() +
                        BusinessLogic.GetInstance().GetNewMessagesCount();

        LocalBroadcast.sendBroadcast(context, new Intent(ACTION_BADGE_UPDATE).putExtra("count", badgeCount));
    }

    private static boolean isCurrentActivity(Context context, Class<? extends Activity> activityClass) {
        return TextUtils.equals(DodicallApplication.getCurrentActivityName(context), activityClass.getName());
    }

    private static boolean shouldShowIncomingCallScreen(Context context) {
        return (
                DodicallApplication.isVisible(context) ||
                        screenLocked(context) ||
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
        );
    }

    private static boolean screenLocked(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !powerManager.isScreenOn();
    }

    private static AudioManager getAudioManager(Context context) {
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        D.log(TAG, "[delete]");
        switch (sUriMatcher.match(uri)) {
            case ID_CONTACTS_URI_TYPE: {
                int contactModelId = Integer.valueOf(uri.getLastPathSegment());
                ContactModel contactModel = mContacts.get(contactModelId);
                if (contactModel == null) {
                    D.log(TAG, "[delete] contactModel not found");
                    return 0;
                }
                D.log(TAG, "[delete] contactModel.firstName: %s", contactModel.getFirstName());
                boolean success;
                if (!TextUtils.isEmpty(selection) &&
                        Integer.valueOf(selection) == FILTER_SUBSCRIPTIONS) {
                    D.log(TAG, "[delete] FILTER_SUBSCRIPTION");
                    success = BusinessLogic.GetInstance().AnswerSubscriptionRequest(contactModel, false);
                } else {
                    success = BusinessLogic.GetInstance().DeleteContact(mContacts.get(contactModelId));
                }
                D.log(TAG, "[delete] success: %s", success);
                if (success) {
                    mContacts.remove(contactModelId);
                    getContext().getContentResolver().notifyChange(CONTACTS_URI, null);

                    LocalBroadcast.sendBroadcast(getContext(), ACTION_CONTACTS_UPDATED);
                    return 1;
                }
                break;
            }

            case CHAT_ID_URI_TYPE: {
                final String chatId = uri.getPathSegments().get(1);
                Chat chatModel = mChats.get(chatId);

                if (chatModel == null) {
                    return 0;
                }

                mChats.remove(chatModel.getId());

                return 1;
            }
            case DROP_DATA_URI_TYPE: {
                if (mIncompleteMessageMap != null) {
                    mIncompleteMessageMap.clear();
                }

                if (mChats != null) {
                    mChats.clear();
                }

                if (mContacts != null) {
                    mContacts.clear();
                }

                if (mDdcContacts != null) {
                    mDdcContacts.clear();
                }
                ContactsManagerImpl.getInstance().clearCache();
                return 1;
            }

            case ACTIVITY_URI_TYPE: {
                mCurrentActiveScreen = null;
                return 1;
            }

            default: {
                throw new IllegalArgumentException(String.format("Wrong uri: %s", uri));
            }
        }
        return 0;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case BASE_CONTACTS_URI_TYPE:
                return CONTACTS_CONTENT_TYPE;
            case ID_CONTACTS_URI_TYPE:
                return CONTACTS_CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        D.log(TAG, "[insert]");
        switch (sUriMatcher.match(uri)) {
            case ID_CONTACTS_URI_TYPE: {
                int currentId = Integer.valueOf(uri.getLastPathSegment());
                D.log(TAG, "[insert] currentId: %d", currentId);
                D.log(TAG, "[insert] contactModel: %s", mContacts.containsKey(currentId));
                ContactModel contact = BusinessLogic.GetInstance().SaveContact(
                        mContacts.get(currentId)
                );
                int newId = contact == null ? 0 : contact.getId();
                D.log(TAG, "[insert] newId: %d", newId);
                if (newId > 0) {
                    mContacts.remove(currentId);
                    getContext().getContentResolver().notifyChange(CONTACTS_URI, null);
                }
                return ContentUris.withAppendedId(DataProvider.CONTACTS_URI, newId);
            }
            case BASE_CONTACTS_URI_TYPE: {
                D.log(TAG, "[insert] create new contactModel");
                ContactModel contactModel = new ContactModel();
                contactModel.setFirstName(values.getAsString(ContactsColumn.FIRSTNAME));
                contactModel.setLastName(values.getAsString(ContactsColumn.LASTNAME));
                ContactsContactSet phonesSet = new ContactsContactSet();
                String[] phones =
                        values.getAsString(ContactsColumn.PHONES_LIST).split(LIST_SEPARATOR);
                ContactsContactModel phoneModel;
                for (String phone : phones) {
                    phoneModel = new ContactsContactModel();
                    phoneModel.setIdentity(phone);
                    phoneModel.setType(ContactsContactType.ContactsContactPhone);
                    phonesSet.insert(phoneModel);
                }
                contactModel.setContacts(phonesSet);
                ContactModel contact = BusinessLogic.GetInstance().SaveContact(contactModel);
                int newId = contact == null ? 0 : contact.getId();
                if (newId > 0) {
                    mContacts.put(
                            newId, BusinessLogic.GetInstance().GetContactByIdFromCache(newId)
                    );
                    getContext().getContentResolver().notifyChange(CONTACTS_URI, null);
                }
                return ContentUris.withAppendedId(DataProvider.CONTACTS_URI, newId);
            }
            case DDC_SEARCH_ID_URI_TYPE: {
                String currentId = uri.getLastPathSegment();
                ContactModel contactModel = mDdcContacts.get(currentId);
                if (contactModel == null) {
                    Contact contact = mOtherContacts.get(currentId);
                    if (contact != null) {
                        contactModel = BusinessLogic.GetInstance().RetriveContactByNumber(contact.sips.get(0));
                    }
                }
                D.log(TAG, "[insert] accept_request; uri: %s; contact: %s", uri, contactModel);
                ContactModel newContact = BusinessLogic.GetInstance().SaveContact(contactModel);
                int newId = newContact == null ? 0 : newContact.getId();
                if (newId > 0) {
                    mDdcContacts.put(currentId, newContact);
                    getContext().getContentResolver().notifyChange(CONTACTS_URI, null);
                }
                return ContentUris.withAppendedId(DataProvider.CONTACTS_URI, newId);
            }
            case CHATS_URI_TYPE: {

                String[] ids = values.getAsString("contactIds").split(",");
                if (ids.length == 0) {
                    return null;
                }

                Chat chat = BL.createChatWithContacts(ids);
                if (chat != null) {
                    prepareChatsContainer();

                    mChats.put(chat.getId(), chat);
                    return makeChatUri(chat.getId());
                }

                return null;
            }
            case CHATS_CONTACTS_URI_TYPE: {
                Contact result = new Contact();

                result.id = values.getAsInteger(ContactsColumn.ID);
                result.dodicallId = values.getAsString(ContactsColumn.DODICALL_ID);
                result.phonebookId = values.getAsString(ContactsColumn.PHONEBOOK_ID);
                result.nativeId = values.getAsString(ContactsColumn.NATIVE_ID);
                result.xmppId = values.getAsString(ContactsColumn.XMPP_ID);
                result.firstName = values.getAsString(ContactsColumn.FIRSTNAME);
                result.lastName = values.getAsString(ContactsColumn.LASTNAME);
                result.middleName = values.getAsString(ContactsColumn.MIDDLENAME);
                result.avatarPath = values.getAsString(ContactsColumn.AVATAR_PATH);
                result.blocked = values.getAsBoolean(ContactsColumn.BLOCKED);
                result.white = values.getAsBoolean(ContactsColumn.WHITE);

                ContactPresenceStatusModel cpsm = getContactStatus(result.xmppId);

                result.contactStatus = new ContactStatus(result.xmppId, cpsm.getBaseStatus().swigValue(), cpsm.getExtStatus());

                result.invite = values.getAsBoolean(ContactsColumn.INVITE);
                result.subscriptionRequest = values.getAsBoolean(ContactsColumn.SUBSCRIPTION_REQUEST);
                result.subscriptionState = values.getAsInteger(DataProvider.ContactsColumn.SUBSCRIPTION_STATE);
                result.newInvite = values.getAsBoolean(ContactsColumn.NEW_INVITE);
                result.directory = values.getAsBoolean(ContactsColumn.DIRECTORY);
                result.iAm = values.getAsBoolean(ContactsColumn.I_AM);
                result.isMine = values.getAsBoolean(ContactsColumn.IS_MINE);
                result.isDeclinedRequest = values.getAsBoolean(ContactsColumn.DECLINED_REQUEST);

                result.phones = new ArrayList<>(Arrays.asList(values.getAsString(ContactsColumn.PHONES_LIST).split(DataProvider.LIST_SEPARATOR)));
                result.sips = new ArrayList<>(Arrays.asList(values.getAsString(ContactsColumn.SIP_LIST).split(DataProvider.LIST_SEPARATOR)));

                mOtherContacts.put(result.dodicallId, result);
                return null;
            }
            default: {
                throw new IllegalArgumentException(String.format("Wrong uri: %s", uri));
            }
        }
    }

    private final class PhonebookObserver extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        private PhonebookObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            D.log(TAG, "[PhonebookObserver.onChange]");
            new Thread(() -> {
                BusinessLogic.GetInstance().CachePhonebookContacts(
                        getPhonebookContacts(), true
                );
            }).start();
        }

    }

    @Override
    public boolean onCreate() {
        D.log(TAG, "[onCreate]");
        businessLogicCallback.addManager(ContactsManagerImpl.getInstance());
        BusinessLogic.GetInstance().SetupCallbackFunction(businessLogicCallback);
        return true;
    }

    @Override
    public Bundle call(@NonNull String method, String args, Bundle extras) {
        D.log(TAG, "[call] method: %s", method);
        if (TextUtils.equals(method, METHOD_START_PHONEBOOK_SYNC)) {
            startPhonebookSync(new Handler());
        }
        return null;
    }

    public static void startPhonebookSync(ContentResolver contentResolver) {
        contentResolver.call(
                DataProvider.CONTACTS_URI,
                DataProvider.METHOD_START_PHONEBOOK_SYNC,
                null, null
        );
    }

    private void startPhonebookSync(final Handler handler) {
        new Thread(() -> {
            ContactModelSet contacts = getPhonebookContacts();
            Logger.onOperationStart("SyncContactsBL");
            BusinessLogic.GetInstance().CachePhonebookContacts(contacts, true);
            Logger.onOperationEnd("SyncContactsBL", (int) contacts.size());
            handler.post(() -> getContext().getContentResolver().registerContentObserver(
                    ContactsContract.Contacts.CONTENT_URI, true,
                    new PhonebookObserver(new Handler())
            ));
        }).start();

        new Thread(() -> {
            prepareChatsContainer();
            ContactsManagerImpl.getInstance().onCallback(BusinessLogicCallback.Event.Contacts, new ArrayList<>());
//            prepareContacts();
            updateHistoryContacts();
        }).start();
    }

    private static class ContactComparator implements Comparator<ContactModel> {

        private Collator mCollator;

        private ContactComparator(String locale) {
            mCollator = Collator.getInstance(new Locale(locale));
            mCollator.setStrength(Collator.PRIMARY);
        }

        @Override
        public int compare(ContactModel lhs, ContactModel rhs) {
            D.log(TAG, "[compare_debug] lfn: %s; rfn: %s", lhs.getFirstName(), rhs.getFirstName());
            return compare(
                    TextUtils.isEmpty(lhs.getFirstName()) ? " " : lhs.getFirstName(),
                    TextUtils.isEmpty(rhs.getFirstName()) ? " " : rhs.getFirstName()
            );
        }

        private int compare(String lfn, String rfn) {
            if (!Character.isLetter(lfn.charAt(0)) && Character.isLetter(rfn.charAt(0))) {
                return 1;
            } else if (!Character.isLetter(lfn.charAt(0)) && !Character.isLetter(rfn.charAt(0))) {
                return mCollator.compare(lfn, rfn);
            } else if (Character.isLetter(lfn.charAt(0)) && !Character.isLetter(rfn.charAt(0))) {
                return -1;
            } else {
                return mCollator.compare(lfn, rfn);
            }
        }

    }

    @Override
    public Cursor query(
            @NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        D.log(TAG, "[query] uri: %s, selection: %s", uri, selection);
        switch (sUriMatcher.match(uri)) {
            case BASE_CONTACTS_URI_TYPE: {
                D.log(TAG, "[query] BASE_CONTACTS_URI_TYPE");
                final MatrixCursor contactModelsCursor = new MatrixCursor(CONTACTS_COLUMN);
                final int filter = TextUtils.isEmpty(selection) ?
                        ToolBarSpinnerAdapter.FILTER_ALL : Integer.valueOf(selection);
                Logger.onOperationStart("GetContacts");
                List<Contact> result = ContactsManagerImpl.getInstance().getContacts(filter, selectionArgs == null ? "" : selectionArgs[0], true, true);
                for (Contact contactModel : result) {
                    contactModelsCursor.addRow(columnValues(contactModel));
                }
                contactModelsCursor.setNotificationUri(getContext().getContentResolver(), uri);
                Logger.onOperationEnd("GetContacts", result.size());
                return contactModelsCursor;
            }
            case DDC_SEARCH_URI_TYPE: {
                final MatrixCursor contactModelsCursor = new MatrixCursor(CONTACTS_COLUMN);
                final String query = selectionArgs == null ? "" : selectionArgs[0];
                if (!TextUtils.isEmpty(query) && query.length() >= MIN_DDC_QUERY_LENGTH) {
                    ContactModelList searchList = new ContactModelList();
                    BusinessLogic.GetInstance().FindContactsInDirectory(
                            searchList, selectionArgs == null ? "" : selectionArgs[0]
                    );
                    mDdcContacts.clear();
                    for (int i = 0; i < searchList.size(); i++) {
                        D.log(TAG, "[query] subscription_debug: %s", searchList.get(i).getSubscription().getSubscriptionState());
                        mDdcContacts.put(
                                searchList.get(i).getDodicallId(),
                                new ContactModel(searchList.get(i))
                        );
                    }
                    final List<ContactModel> result = new ArrayList<>(mDdcContacts.values());
                    D.log(TAG, "[query] ddc_search result: %d", result.size());
                    Collections.sort(result, new ContactComparator(Utils.getLocale(getContext())));
                    StringSet ids = new StringSet();
                    for (ContactModel contact : result) {
                        if (!TextUtils.isEmpty(contact.getDodicallId()) && TextUtils.isEmpty(contact.getAvatarPath())) {
                            ids.insert(contact.getDodicallId());
                        }
                        contactModelsCursor.addRow(columnValues(contact));
                    }
                    if (!ids.empty()) {
                        BusinessLogic.GetInstance().DownloadAvatarForContactsWithDodicallIds(ids);
                    }
                }
                contactModelsCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return contactModelsCursor;
            }
            case DDC_SEARCH_ID_URI_TYPE: {
                D.log(TAG, "[query] ID_CONTACTS_URI_TYPE: %s;", uri);
                final MatrixCursor singleContactCursor = new MatrixCursor(CONTACTS_COLUMN);
                final ContactModel contactModel = mDdcContacts.get(uri.getLastPathSegment());
                if (contactModel != null) {
                    singleContactCursor.addRow(columnValues(contactModel));
                } else {
                    Contact contact = mOtherContacts.get(uri.getLastPathSegment());
                    if (contact != null) {
                        singleContactCursor.addRow(columnValues(contact));
                    }
                }

                singleContactCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return singleContactCursor;
            }
            case ID_CONTACTS_URI_TYPE: {
                D.log(TAG, "[query] ID_CONTACTS_URI_TYPE: %s;", uri);
                final MatrixCursor singleContactCursor = new MatrixCursor(CONTACTS_COLUMN);
                final ContactModel contactModel = mContacts.get(
                        Integer.valueOf(uri.getLastPathSegment())
                );
                if (contactModel == null) {
                    D.log(TAG, "[query] contact is null");
                    singleContactCursor.setNotificationUri(getContext().getContentResolver(), uri);
                    return singleContactCursor;
                }
                singleContactCursor.addRow(columnValues(contactModel));
                singleContactCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return singleContactCursor;
            }
            case MY_CONTACT_URI_TYPE: {
                final MatrixCursor myContactCursor = new MatrixCursor(CONTACTS_COLUMN);
                ContactModel contact = BusinessLogic.GetInstance().GetAccountData();
                myContactCursor.addRow(columnValues(contact, true));
                myContactCursor.setNotificationUri(getContext().getContentResolver(), uri);
                return myContactCursor;
            }

            case RETRIEVE_NUMBER: {
                String number = uri.getLastPathSegment();

                final MatrixCursor cursor = new MatrixCursor(CONTACTS_COLUMN);
                ContactModel contact = BusinessLogic.GetInstance().RetriveContactByNumber(number);

                if (!TextUtils.isEmpty(contact.getDodicallId()) ||
                        !TextUtils.isEmpty(contact.getNativeId()) ||
                        !TextUtils.isEmpty(contact.getPhonebookId())) {
                    cursor.addRow(columnValues(contact));
                }

                return cursor;
            }

            case INVITES_COUNT_TYPE: {
                final MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_INVITES_COUNT});
                int invitesCount = ContactsManagerImpl.getInstance().getNewInvites().size();
                D.log(TAG, "[query] invites_count: %d", invitesCount);
                cursor.addRow(new Object[]{invitesCount});
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
            }

            case CURRENT_CALL_URI_TYPE: {
                final MatrixCursor callCursor = new MatrixCursor(CURRENT_CALL_COLUMNS);
                final MatrixCursor contactCursor = new MatrixCursor(CONTACTS_COLUMN);
                final List<Call> calls = BL.getCalls();
                D.log(TAG, "[query][call_contact_name_debug] current_call");
                if (!calls.isEmpty()) {
                    final Call call = calls.get(0);
                    callCursor.addRow(columnValues(call));
                    if (call.contact != null) {
                        D.log(TAG, "[query][call_contact_name_debug] current_call; contact is present");
                        Contact contact = call.contact;
                        contactCursor.addRow(columnValues(contact));
                    }
                }
                final MergeCursor cursor = new MergeCursor(
                        new Cursor[]{callCursor, contactCursor}
                );
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
                return cursor;
            }
            case CHATS_URI_TYPE: {
                Logger.onOperationStart("LoadChatsProvider");
                final MatrixCursor chatsCursor = new MatrixCursor(CHAT_COLUMNS);

                prepareChatsContainer();

                synchronized (mChatAccessLock) {
                    for (Chat chat : mChats.values()) {
                        ChatMessage lastMessage = chat.getLastMessage();
                        String incompleteMessage = mIncompleteMessageMap.get(chat.getId());
                        if ((lastMessage != null) || !TextUtils.isEmpty(incompleteMessage)) {
                            chatsCursor.addRow(columnValues(chat, incompleteMessage));
                        }
                    }
                }
                Logger.onOperationEnd("LoadChatsProvider");

                return chatsCursor;
            }

            case CHAT_ID_URI_TYPE: {
                final MatrixCursor chatsCursor = new MatrixCursor(CHAT_COLUMNS);
                final String chatId = uri.getPathSegments().get(1);

                prepareChatsContainer();

                synchronized (mChatAccessLock) {
                    Chat chatModel = mChats.get(chatId);

                    if (chatModel == null) {
                        chatModel = BL.getChatById(chatId);

                        if (chatModel != null) {
                            mChats.put(chatModel.getId(), chatModel);

                            chatsCursor.addRow(columnValues(chatModel, mIncompleteMessageMap.get(chatId)));
                        }
                    }
                    chatsCursor.addRow(columnValues(chatModel, mIncompleteMessageMap.get(chatId)));
                }

                return chatsCursor;
            }

            case CHATS_CONTACTS_URI_TYPE: {
                Logger.onOperationStart("LoadChatContactsProvider");
                final MatrixCursor contactModelsCursor = new MatrixCursor(CONTACTS_COLUMN);
                final String chatId = uri.getPathSegments().get(1);

                Chat chatModel = mChats.get(chatId);

                if (chatModel == null) {
                    chatModel = BL.getChatById(chatId);
                }

                if (chatModel != null) {
                    ArrayList<Contact> contacts = chatModel.getContacts();
                    for (Contact contact : contacts) {
                        contactModelsCursor.addRow(columnValues(contact));
                    }
                }
                Logger.onOperationEnd("LoadChatContactsProvider");

                return contactModelsCursor;
            }

            case CHATS_MESSAGES_LAST_URI_TYPE: {
                Logger.onOperationStart("LoadLastMessageProvider");
                final MatrixCursor lastMessageCursor = new MatrixCursor(MESSAGE_COLUMNS);
                final String chatId = uri.getPathSegments().get(1);

                Chat chatModel = mChats.get(chatId);
                ChatMessage lastMessage = null;
                if (chatModel == null) {
                    chatModel = BL.getChatById(chatId);
                    if (chatModel != null) {
                        lastMessage = chatModel.getLastMessage();
                    }
                } else {
                    lastMessage = chatModel.getLastMessage();
                }

                if (lastMessage != null) {
                    lastMessageCursor.addRow(columnValues(lastMessage));
                }
                Logger.onOperationEnd("LoadLastMessageProvider");

                return lastMessageCursor;
            }

            case CHAT_MESSAGES_SENDER_URI_TYPE: {
                final MatrixCursor senderCursor = new MatrixCursor(CONTACTS_COLUMN);
                final String chatId = uri.getPathSegments().get(1);

                Chat chatModel = mChats.get(chatId);
                ChatMessage lastMessage = chatModel.getLastMessage();
                if (lastMessage == null) {
                    chatModel = BL.getChatById(chatId);
                    lastMessage = chatModel.getLastMessage();
                }

                if (lastMessage != null) {
                    senderCursor.addRow(columnValues(lastMessage.getSender()));
                }

                return senderCursor;
            }

            case CHAT_MESSAGES_NOTIFICATION_DATA_URI_TYPE: {
                final MatrixCursor ndCursor = new MatrixCursor(NOTIFICATION_DATA_COLUMNS);
                final String chatId = uri.getPathSegments().get(1);

                Chat chatModel = mChats.get(chatId);
                ChatMessage lastMessage = chatModel.getLastMessage();
                if (lastMessage == null) {
                    chatModel = BL.getChatById(chatId);
                    lastMessage = chatModel.getLastMessage();
                }

                if (lastMessage != null) {
                    if (lastMessage.getNotificationData() != null) {
                        ChatNotificationData cnd = lastMessage.getNotificationData();
                        ndCursor.addRow(columnValues(cnd, chatId));
                    }
                }

                return ndCursor;
            }

            case CHAT_MESSAGES_NOTIFICATION_DATA_CONTACTS_URI_TYPE: {
                final MatrixCursor cursor = new MatrixCursor(CONTACTS_COLUMN);
                final String chatId = uri.getPathSegments().get(1);

                Chat chatModel = mChats.get(chatId);
                ChatMessage lastMessage = chatModel.getLastMessage();
                if (lastMessage == null) {
                    chatModel = BL.getChatById(chatId);

                    lastMessage = chatModel.getLastMessage();
                }

                if (lastMessage != null) {
                    if (lastMessage.getNotificationData() != null) {
                        ChatNotificationData cnd = lastMessage.getNotificationData();
                        ArrayList<Contact> contacts = cnd.getContacts();
                        for (Contact contact : contacts) {
                            cursor.addRow(columnValues(contact));
                        }
                    }
                }

                return cursor;
            }

            case ME_URI_TYPE: {
                final MatrixCursor cursor = new MatrixCursor(CONTACTS_COLUMN);
                final ContactModel me = BusinessLogic.GetInstance().GetAccountData();

                cursor.addRow(columnValues(me));

                return cursor;
            }
            case STATUS_URI_TYPE: {
                final MatrixCursor cursor = new MatrixCursor(new String[]{ContactsColumn.BASE_STATUS, ContactsColumn.EXTRA_STATUS});
                String xmppId = uri.getLastPathSegment();
                ContactPresenceStatusModel presenceStatusModel = mContactStatuses.get(xmppId);
                if (presenceStatusModel == null) {
                    presenceStatusModel = BusinessLogic.GetInstance().GetPresenceStatusByXmppId(xmppId);
                    mContactStatuses.put(xmppId, new ContactPresenceStatusModel(xmppId, presenceStatusModel.getBaseStatus(), presenceStatusModel.getExtStatus()));
                }
                cursor.addRow(new Object[]{presenceStatusModel.getBaseStatus().swigValue(), presenceStatusModel.getExtStatus()});
                return cursor;
            }

            default:
                throw new IllegalArgumentException("Wrong uri: " + uri);
        }
    }

    private boolean prepareChatsContainer() {
        if (mChats == null) {
            synchronized (mChatAccessLock) {
                if (mChats == null) {
                    mChats = new ConcurrentHashMap<>();

                    for (Chat chat : BL.getChats()) {
                        mChats.put(chat.getId(), chat);
                        extractChatContacts(chat);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    private void extractChatContacts(Chat chat) {
        Stream.of(chat.getContacts()).filter(contact -> !contact.iAm && contact.directory).forEach(contact -> ContactsManagerImpl.getInstance().addOtherContact(contact)/*mOtherContacts.put(contact.dodicallId, contact)*/);
    }

    private void updateHistoryContacts() {
        CallHistoriesList h = BL.getCallHistoriesList();

        Stream.of(h)
                .filter(value -> value.contact != null && value.contact.isDodicall())
                .forEach(callHistory -> ContactsManagerImpl.getInstance().addOtherContact(callHistory.contact));
    }

    private static Object[] columnValues(Chat chatModel, String incompleteMessage) {
        return new Object[]{
                chatModel.getId(),
                chatModel.getTitle(),
                chatModel.isActive() ? 1 : 0,
                chatModel.getTotalMessagesCount(),
                chatModel.getNewMessagesCount(),
                chatModel.getLastModifiedDate(),
                chatModel.isP2p() ? 1 : 0,
                incompleteMessage
        };
    }

    private static Object[] columnValues(ChatMessage messageModel) {
        return new Object[]{
                messageModel.getId(),
                messageModel.getChatId(),
                messageModel.isServered() ? 1 : 0,
                messageModel.getSendTime(),
                messageModel.isRead() ? 1 : 0,
                messageModel.isEdited() ? 1 : 0,
                messageModel.getContent(),
                messageModel.hasQuotedMessages() ? ChatMessage.TYPE_QUOTED : messageModel.getType(),
                messageModel.getRownum(),
                messageModel.isEncrypted() ? 1 : 0,
        };
    }

    private static Object[] columnValues(ChatNotificationData notificationData, String chatMessageId) {
        return new Object[]{notificationData.getType(), chatMessageId};
    }

    private static Object[] columnValues(Contact contact) {
        return columnValues(contact, false);
    }

    private static Object[] columnValues(Contact contact, boolean my) {
        ContactPresenceStatusModel status;
        if (my) {
            status = getUserStatus(ID_MY);
        } else {
            status = getUserStatus(contact.xmppId);
        }

        return new Object[]{
                contact.id, contact.dodicallId,
                contact.phonebookId, contact.nativeId, contact.xmppId,
                contact.firstName, contact.lastName,
                contact.middleName,
                contact.avatarPath,
                contact.id != 0 && contact.blocked ? 1 : 0,
                contact.white ? 1 : 0,
                status.getBaseStatus().swigValue(),
                status.getExtStatus(),
                contact.invite ? 1 : 0,
                contact.subscriptionRequest ? 1 : 0,
                contact.newInvite ? 1 : 0,
                contact.directory ? 1 : 0,
                contact.iAm ? 1 : 0,
                contact.isMine ? 1 : 0,
                contact.isDeclinedRequest ? 1 : 0,
                TextUtils.join(LIST_SEPARATOR, contact.sips),
                TextUtils.join(LIST_SEPARATOR, contact.phones),
                contact.subscriptionState
        };
    }

    private Object[] columnValues(ContactModel contactModel) {
        return columnValues(contactModel, false);
    }

    private Object[] columnValues(ContactModel contactModel, boolean my) {
        ContactPresenceStatusModel status;
        if (my) {
            status = getContactStatus(ID_MY);
        } else {
            status = getContactStatus(contactModel.GetXmppId());
        }
        ContactsContactList contactModels =
                BusinessLogic.GetInstance().GetContacts(contactModel);
        return new Object[]{
                contactModel.getId(), contactModel.getDodicallId(),
                contactModel.getPhonebookId(), contactModel.getNativeId(), contactModel.GetXmppId(),
                contactModel.getFirstName(), contactModel.getLastName(),
                contactModel.getMiddleName(),
                contactModel.getAvatarPath(),
                (isBlocked(contactModel) ? 1 : 0),
                (contactModel.getWhite() ? 1 : 0),
                status.getBaseStatus().swigValue(),
                status.getExtStatus(),
                (isInvite(contactModel) ? 1 : 0),
                (contactModel.getSubscription().getAskForSubscription() ? 1 : 0),
                (isNewInvite(contactModel) ? 1 : 0),
                isDirectory(contactModel) ? 1 : 0,
                contactModel.getIam() ? 1 : 0,
                BL.isMineContact(contactModel) ? 1 : 0,
                isDeclinedRequest(contactModel) ? 1 : 0,
                getSipNumbers(contactModels),
                getPhoneNumbers(contactModels),
                contactModel.getSubscription().getSubscriptionState().swigValue()
        };
    }

    private static Object[] columnValues(Call call) {
        return new Object[]{
                call.id,
                call.direction,
                call.encryption,
                call.state,
                call.duration,
                call.identity,
                call.addressType,
                null
        };
    }

    private ContactPresenceStatusModel getContactStatus(@Nullable String xmppId) {
        if (TextUtils.isEmpty(xmppId) || !mContactStatuses.containsKey(xmppId)) {
            return new ContactPresenceStatusModel("", BaseUserStatus.BaseUserStatusOffline, "");
        }
        D.log(TAG, "[getUserStatus] xmppId: %s", xmppId);
        return mContactStatuses.get(xmppId);
    }

    public static ContactPresenceStatusModel getUserStatus(@Nullable String xmppId) {
        if (TextUtils.isEmpty(xmppId)) {
            return new ContactPresenceStatusModel("", BaseUserStatus.BaseUserStatusOffline, "");
        }
        D.log(TAG, "[getUserStatus] xmppId: %s", xmppId);
        return BusinessLogic.GetInstance().GetPresenceStatusByXmppId(xmppId);
    }

    private static String getSipNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactSip);
    }

    private static String getPhoneNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactPhone);
    }

    private static String getNumbers(
            ContactsContactList contactModels, ContactsContactType type) {
        StringBuilder numbers = new StringBuilder();
        ContactsContactModel contactModel;
        for (int i = 0; i < contactModels.size(); i++) {
            contactModel = contactModels.get(i);
            if (contactModel.getType() == type) {
                if (contactModel.getFavourite() &&
                        contactModel.getType() == ContactsContactType.ContactsContactSip) {
                    numbers.append(FAVORITE_MARKER);
                }
                numbers.append(contactModel.getIdentity());
                if (i + 1 < contactModels.size()) {
                    numbers.append(LIST_SEPARATOR);
                }
            }
        }
        return numbers.toString();
    }

    @Override
    public int update(
            @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        switch (sUriMatcher.match(uri)) {
            case ID_CONTACTS_URI_TYPE: {
                ContactModel contactModel = mContacts.get(Integer.valueOf(uri.getLastPathSegment()));
                if (contactModel == null) {
                    return 0;
                }
                if (values.containsKey(ContactsColumn.FIRSTNAME) || values.containsKey(ContactsColumn.LASTNAME)) {
                    contactModel.setFirstName(values.getAsString(ContactsColumn.FIRSTNAME));
                    contactModel.setLastName(values.getAsString(ContactsColumn.LASTNAME));
                    D.log(TAG, "[update] firstName: %s;", values.getAsString(ContactsColumn.FIRSTNAME));
                    ContactsContactSet phonesSet = new ContactsContactSet();
                    String[] phones =
                            values.getAsString(ContactsColumn.PHONES_LIST).split(LIST_SEPARATOR);
                    ContactsContactModel phoneModel;
                    for (String phone : phones) {
                        phoneModel = new ContactsContactModel();
                        phoneModel.setIdentity(phone);
                        phoneModel.setType(ContactsContactType.ContactsContactPhone);
                        phoneModel.setManual(true);
                        phonesSet.insert(phoneModel);
                    }
                    contactModel.setContacts(phonesSet);
                    D.log(TAG, "[update] edit_contactModel; oldId: %d;", contactModel.getId());
                    ContactModel contact = BusinessLogic.GetInstance().SaveContact(contactModel);
                    int newId = contact == null ? 0 : contact.getId();
                    D.log(TAG, "[update] edit_contactModel; newId: %d;", newId);
                    getContext().getContentResolver().notifyChange(uri, null);
                    return 1;
                } else if (values.containsKey(FAVORITE_MARKER)) {
                    ContactsContactList contactModels = BusinessLogic.GetInstance().GetContacts(contactModel);
                    String sipFavorite = values.getAsString(FAVORITE_MARKER).replace(FAVORITE_MARKER, "");
                    D.log(TAG, "[update][favorite_debug] number: %s", sipFavorite);
                    ContactsContactSet newContacts = new ContactsContactSet();
                    for (int i = 0; i < contactModels.size(); i++) {
                        contactModels.get(i).setFavourite(
                                contactModels.get(i).getIdentity().equals(sipFavorite)
                        );
                        newContacts.insert(contactModels.get(i));
                    }
                    contactModel.setContacts(newContacts);
                    BusinessLogic.GetInstance().SaveContact(contactModel);
                    getContext().getContentResolver().notifyChange(uri, null);
                    return 1;
                } else if (values.containsKey(ContactsColumn.BLOCKED)) {
                    setBlocked(contactModel, values.getAsBoolean(ContactsColumn.BLOCKED));
                    getContext().getContentResolver().notifyChange(uri, null);
                    return 1;
                } else if (values.containsKey(ContactsColumn.NEW_INVITE)) {
                    //always must be false
                    boolean read = values.getAsBoolean(ContactsColumn.NEW_INVITE);
                    contactModel.getSubscription().setSubscriptionStatus(
                            ContactSubscriptionStatus.ContactSubscriptionStatusReaded
                    );
                    boolean success = true;
                    if (!read) {
                        success = BusinessLogic.GetInstance().MarkSubscriptionAsOld(
                                contactModel.GetXmppId()
                        );
                        D.log(TAG, "[update] new_invite; success: %s", success);
                    }
                    if (success) {
                        getContext().getContentResolver().notifyChange(uri, null);
                        return 1;
                    } else {
                        return 0;
                    }
                } else if (values.containsKey(ContactsColumn.WHITE)) {
                    boolean white = values.getAsBoolean(ContactsColumn.WHITE);
                    contactModel.setWhite(white);
                    if (white) {
                        contactModel.setBlocked(false);
                    }
                    BusinessLogic.GetInstance().SaveContact(contactModel);
                    getContext().getContentResolver().notifyChange(uri, null);
                    return 1;
                }
            }
            case DDC_SEARCH_ID_URI_TYPE: {
                ContactModel contactModel = mDdcContacts.get(uri.getLastPathSegment());
                if (contactModel == null) {
                    return 0;
                }
                if (values.containsKey(ContactsColumn.BLOCKED)) {
                    D.log(TAG, "[update] block: %s", values.getAsBoolean(ContactsColumn.BLOCKED));
                    setBlocked(contactModel, values.getAsBoolean(ContactsColumn.BLOCKED));
                    getContext().getContentResolver().notifyChange(uri, null);
                    return 1;
                }
            }
            case CHATS_MESSAGES_SET_READ: {
                final String chatId = uri.getPathSegments().get(1);
                final String messageId = uri.getLastPathSegment();

                if (BusinessLogic.GetInstance().MarkMessagesAsReaded(messageId)) {
                    prepareChatsContainer();

                    synchronized (mChatAccessLock) {
                        Chat chat = BL.getChatById(chatId);
                        if (chat != null) {
                            mChats.put(chatId, chat);
                            sendLocalBroadcastForIds(getContext(), ACTION_CHATS_UPDATED, chatId);
                        }
                    }
                }
                return 1;
            }
            case CHAT_INCOMPLETE_MESSAGE_URI_TYPE: {
                final String chatId = uri.getPathSegments().get(1);
                String incompleteMessage = values.getAsString(Chat.COLUMN_INCOMPLETE_MESSAGE);
                mIncompleteMessageMap.put(chatId, incompleteMessage);
                sendLocalBroadcastForIds(getContext(), ACTION_CHATS_UPDATED, chatId);
                return 1;
            }

            case CHAT_ID_URI_TYPE: {
                if (TextUtils.equals(selection, SELECTION_INVITE_AND_REVOKE_CHAT_MEMBERS)) {
                    final String chatId = uri.getLastPathSegment();
                    String inviteIds = values.getAsString("inviteIds");
                    String revokeIds = values.getAsString("revokeIds");

                    ContactModelSet inviteModelSet = new ContactModelSet();
                    ContactModelSet revokeModelSet = new ContactModelSet();

                    if (!TextUtils.isEmpty(inviteIds)) {
                        String[] splitIds = inviteIds.split(",");
                        for (String id : splitIds) {
                            ContactModel c = GetContactByIdFromCache(Integer.valueOf(id));
                            if (c != null) {
                                inviteModelSet.insert(c);
                            }
                        }
                    }

                    if (!TextUtils.isEmpty(revokeIds)) {
                        String[] splitIds = revokeIds.split(",");
                        for (String id : splitIds) {
                            ContactModel c = GetContactByIdFromCache(Integer.valueOf(id));
                            if (c != null) {
                                revokeModelSet.insert(c);
                            }
                        }
                    }

                    String updatedChatId = BusinessLogic.GetInstance().InviteAndRevokeChatMembers(chatId, inviteModelSet, revokeModelSet);
                    if (!TextUtils.isEmpty(updatedChatId)) {
                        prepareChatsContainer();

                        synchronized (mChatAccessLock) {
                            if (chatId.equals(updatedChatId)) {
                                Chat chat = BL.getChatById(chatId);
                                if (chat != null) {
                                    mChats.put(chatId, chat);
                                    sendLocalBroadcastForIds(getContext(), ACTION_CHATS_UPDATED, chatId);
                                }
                            } else {
                                sendLocalBroadcastForIds(getContext(), ACTION_CHATS_RECREATED, chatId, updatedChatId);
                            }
                        }
                    }

                    return 1;
                }

                return 0;
            }

            case ACTIVITY_URI_TYPE: {
                final String activityType = uri.getPathSegments().get(1);
                NotificationsUtils.NotificationType notificationType = NotificationsUtils.NotificationType.valueOf(activityType);
                String activityId = values.getAsString(CurrentActivityColumns.ACTIVITY_ID);
                mCurrentActiveScreen = Pair.create(notificationType, activityId);
                boolean isWithNotificationUpdate = values.getAsBoolean(CurrentActivityColumns.NOTIFICATION_UPDATE_NEEDED);
                if (isWithNotificationUpdate) {
                    updateNotificationByType(notificationType, activityId);
                }
                return 1;
            }

            default: {
                throw new IllegalArgumentException(String.format("Wrong uri: %s", uri));
            }
        }
    }

    private ContactModel GetContactByIdFromCache(int id) {
        ContactModel c = BusinessLogic.GetInstance().GetContactByIdFromCache(id);
        return isValidContact(c)
                ? c
                : null;
    }

    private boolean isValidContact(ContactModel c) {
        return !TextUtils.isEmpty(c.getDodicallId()) ||
                !TextUtils.isEmpty(c.getNativeId()) ||
                !TextUtils.isEmpty(c.getPhonebookId());
    }

    private static void setBlocked(ContactModel contactModel, boolean blockedValue) {
        contactModel.setBlocked(blockedValue);
        BusinessLogic.GetInstance().SaveContact(contactModel);
    }

    @Nullable
    public static String[] extractExtraIds(Intent intent) {
        return intent.getStringArrayExtra(EXTRA_IDS);
    }

    private static boolean isFromPhonebook(ContactModel contactModel) {
        return !TextUtils.isEmpty(contactModel.getPhonebookId());
    }

    private static boolean isSaved(ContactModel contactModel) {
        return !TextUtils.isEmpty(contactModel.getNativeId());
    }

    public static boolean isBlocked(ContactModel contactModel) {
        return (contactModel.getId() != 0 && contactModel.getBlocked());
    }

    public static boolean isInvite(ContactModel contactModel) {
        return (
                contactModel.getId() == 0 &&
                        contactModel.getSubscription().getSubscriptionState() ==
                                ContactSubscriptionState.ContactSubscriptionStateTo
        );
    }

    public static boolean isDirectory(ContactModel contactModel) {
        return (
                contactModel.getId() == 0 &&
                        contactModel.getSubscription().getSubscriptionState() ==
                                ContactSubscriptionState.ContactSubscriptionStateNone
        );
    }

    public static boolean isNewInvite(ContactModel contactModel) {
        return (
                isInvite(contactModel) &&
                        contactModel.getSubscription().getSubscriptionStatus() ==
                                ContactSubscriptionStatus.ContactSubscriptionStatusNew
        );
    }

    public static boolean isDeclinedRequest(ContactModel contactModel) {
        return (
                contactModel.getSubscription().getSubscriptionState() ==
                        ContactSubscriptionState.ContactSubscriptionStateFrom &&
                        !contactModel.getSubscription().getAskForSubscription()
        );
    }

    private ContactModelSet getPhonebookContacts() {
        ContactModelSet contactModels = new ContactModelSet();
        Logger.onOperationStart("SyncContacts");
        Cursor contactModelCursor = getContext().getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
                },
                ContactsContract.Contacts.IN_VISIBLE_GROUP + " = 1 AND " +
                        ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1",
                null, null
        );
        if (contactModelCursor != null) {
            try {
                if (contactModelCursor.moveToFirst()) {
                    do {
                        ContactModel contactModel = new ContactModel();
                        Logger.onOperationStart("SyncContactsDecodeContact");
                        String id = contactModelCursor.getString(
                                contactModelCursor.getColumnIndex(ContactsContract.Contacts._ID)
                        );
                        String contactModelName = contactModelCursor.getString(contactModelCursor.getColumnIndex(
                                ContactsContract.Data.DISPLAY_NAME_PRIMARY
                        ));
                        Cursor phonesCursor = getContext().getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null
                        );
                        if (phonesCursor != null) {
                            try {
                                if (phonesCursor.moveToFirst()) {
                                    ContactsContactSet phones = new ContactsContactSet();
                                    do {
                                        ContactsContactModel phone = new ContactsContactModel();
                                        String phoneNumber = phonesCursor.getString(
                                                phonesCursor.getColumnIndex(
                                                        ContactsContract.CommonDataKinds.Phone.NUMBER
                                                )
                                        );
                                        D.log(
                                                TAG, "[getPhonebookContacts] id: %s; contactModelName: %s; phoneNumber: %s",
                                                id, contactModelName, phoneNumber
                                        );
                                        phone.setType(ContactsContactType.ContactsContactPhone);
                                        phone.setIdentity(phoneNumber);
                                        phones.insert(phone);
                                    } while (phonesCursor.moveToNext());
                                    contactModel.setContacts(phones);
                                }
                            } finally {
                                phonesCursor.close();
                            }
                        }
                        contactModel.setFirstName(contactModelName);
                        contactModel.setPhonebookId(id);
                        contactModels.insert(contactModel);
                        Logger.onOperationEnd("SyncContactsDecodeContact");
                    } while (contactModelCursor.moveToNext());
                }
            } finally {
                contactModelCursor.close();
            }
        }
        Logger.onOperationEnd("SyncContacts", (int) contactModels.size());
        return contactModels;
    }

    public static void sendLocalBroadcastForIds(Context context, String action, ArrayList<String> ids) {
        String[] deletedIdsArray = new String[ids.size()];
        LocalBroadcast.sendBroadcast(context, new Intent(action).putExtra(DataProvider.EXTRA_IDS, ids.toArray(deletedIdsArray)));
    }

    public static void sendLocalBroadcastForIds(Context context, String action, String... ids) {
        LocalBroadcast.sendBroadcast(context, new Intent(action).putExtra(DataProvider.EXTRA_IDS, ids));
    }

    private void updateNotificationByType(@Nullable NotificationsUtils.NotificationType notificationType, String... ids) {
        if (notificationType != null) {
            NotificationsUtils.cancelNotification(getContext(), notificationType);

            if (notificationType == NotificationsUtils.NotificationType.Chat) {
                updateChatNotification(true, ids);
            } else if (notificationType == NotificationsUtils.NotificationType.MissedCall) {
                updateHistoryNotification();
            } else if (notificationType == NotificationsUtils.NotificationType.Invite) {
                updateInviteNotification();
            }
        }
    }

    private void updateChatNotification(boolean withUpdate, String... ids) {
        if (getContext() == null || mChats == null) {
            return;
        }

        ArrayList<Chat> chats = new ArrayList<>(mChats.values());

        if (CollectionUtils.isEmpty(chats)) {
            return;
        }

        NotificationsUtils.NotificationType notificationType = null;
        String chatId = null;
        if (mCurrentActiveScreen != null) {
            notificationType = mCurrentActiveScreen.first;
            if (notificationType == NotificationsUtils.NotificationType.Chat) {
                chatId = (String) mCurrentActiveScreen.second;
            }
        }

        for (int i = chats.size() - 1; i >= 0; i--) {
            Chat c = chats.get(i);
            if (c.getNewMessagesCount() == 0) {
                chats.remove(i);
            } else if (c.getId().equals(chatId)) {
                chats.remove(i); // activity for specified chat exists
            } else if (c.getLastMessage().isDeletedMessage()) {
                chats.remove(i);
            }
        }

        if (chats.isEmpty()) {
            NotificationsUtils.cancelNotification(getContext(), NotificationsUtils.NotificationType.Chat);
            return;
        }

        boolean withHeadsUp = true;
        if (chatId != null && ids.length == 1) {
            String updatedChatId = ids[0];
            if (updatedChatId != null && chatId.equals(updatedChatId)) {  //if updated chats contains ONLY current open chat - do not show heads-up
                withHeadsUp = false;
            }
        }

        if (!(withHeadsUp || withUpdate)) {
            return;
        }

        if (notificationType == null || //current activity is not trackable
                notificationType != NotificationsUtils.NotificationType.Chat || //current activity is not chat activity
                chatId != null) { //current activity is chat but chat is specified
            int ringerMode = getAudioManager(getContext()).getRingerMode();

            NotificationsUtils.createChatNotification(getContext(), chats, withHeadsUp, ringerMode != AudioManager.RINGER_MODE_SILENT, ringerMode == AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private void updateHistoryNotification() {
        if (getContext() == null) {
            return;
        }

        NotificationsUtils.NotificationType notificationType = null;
        String historyId = null;
        if (mCurrentActiveScreen != null) {
            notificationType = mCurrentActiveScreen.first;
            if (notificationType == NotificationsUtils.NotificationType.MissedCall) {
                historyId = (String) mCurrentActiveScreen.second;
            }
        }

        CallHistoriesList list = BL.getCallHistoriesList();

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            CallHistory c = list.get(i);
            if (c.id.equals(historyId)) {
                list.remove(i); // activity for specified history exists
                break;
            }
        }

        if (notificationType == null || //current activity is not trackable
                notificationType != NotificationsUtils.NotificationType.MissedCall || //current activity is not history activity
                historyId != null) { //current activity is history but user is specified
            NotificationsUtils.createHistoryNotification(getContext(), list, getAudioManager(getContext()).getRingerMode() != AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void updateInviteNotification() {
        if (getContext() == null) {
            return;
        }

        if (mCurrentActiveScreen != null && mCurrentActiveScreen.first == NotificationsUtils.NotificationType.Invite) {
            return;
        }

        List<Contact> list = ContactsManagerImpl.getInstance().getNewInvites();

//        Stream.of(mContacts.values()).filter(DataProvider::isNewInvite).forEach(cm -> list.add(BL.newContact(cm)));
//        Stream.of(mDdcContacts.values()).filter(DataProvider::isNewInvite).forEach(cm -> list.add(BL.newContact(cm)));

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        int ringerMode = getAudioManager(getContext()).getRingerMode();

        NotificationsUtils.createInviteNotification(getContext(), list, ringerMode != AudioManager.RINGER_MODE_SILENT, ringerMode == AudioManager.RINGER_MODE_NORMAL);
    }
}
