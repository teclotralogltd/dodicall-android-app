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

package ru.swisstok.dodicall.util;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.swisstok.dodicall.BuildConfig;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ActiveCallActivity;
import ru.swisstok.dodicall.activity.ChatActivity;
import ru.swisstok.dodicall.activity.IncomingCallActivity;
import ru.swisstok.dodicall.activity.MainActivity;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.CallHistoriesList;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.service.HandsUpActionsService;
import ru.uls_global.dodicall.BusinessLogic;

public class NotificationsUtils {

    public enum NotificationType {
        WithoutNotification, Chat, MissedCall, Invite, Call
    }

    //Chats notifications
    private static final int CHATS_INVITATION_NOTIFICATION_ID = 4233;
    private static final int CHATS_NOTIFICATION_ID = 4230;
    private static final int HISTORY_NOTIFICATION_ID = 4231;
    private static final int INVITATION_NOTIFICATION_ID = 4232;
    private static final int CALL_NOTIFICATION_ID = 1001;

    private static final Uri INCOMING_MESSAGE_WAV_URI = Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.incoming_message);

    private static Map<NotificationType, String> sPendingAvatarsForNotifications = new HashMap<>();

    public static NotificationType checkContactForPendingNotification(String dodicallId) {
        for (Map.Entry<NotificationType, String> entry : sPendingAvatarsForNotifications.entrySet()) {
            if (entry.getValue().equals(dodicallId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void cancelNotification(@NonNull Context context, NotificationType... notificationTypes) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationTypes.length == 0) {
            notificationTypes = NotificationType.values();
        }
        for (NotificationType notificationType : notificationTypes) {
            switch (notificationType) {
                case Chat:
                    notificationManager.cancel(CHATS_NOTIFICATION_ID);
                    notificationManager.cancel(CHATS_INVITATION_NOTIFICATION_ID);
                    break;
                case MissedCall:
                    notificationManager.cancel(HISTORY_NOTIFICATION_ID);
                    break;
                case Invite:
                    notificationManager.cancel(INVITATION_NOTIFICATION_ID);
                    break;
                case Call:
                    notificationManager.cancel(CALL_NOTIFICATION_ID);
                    break;
            }
            sPendingAvatarsForNotifications.remove(notificationType);
        }
    }

    public static void createChatNotification(Context context, List<Chat> chats, boolean withHeadsUp, boolean vibrate, boolean withSound) {
        sPendingAvatarsForNotifications.remove(NotificationType.Chat);
        if (chats.isEmpty()) {
            return;
        }

        NotificationCompat.Builder notificationBuilder;

        if (chats.size() == 1) {
            Chat chat = chats.get(0);
            ChatMessage chatMessage = chat.getLastMessage();
            if (chatMessage.isNotificationMessage()) {
                if (checkNotification(chatMessage)) {
                    createInvitationNotification(context, Collections.singletonList(chat), withHeadsUp && vibrate, withSound);
                    return;
                }
            }

            Bundle extras = new Bundle();
            extras.putParcelable(ChatActivity.EXTRA_CHAT, Parcels.wrap(chat));
            Contact p2pChatPartner = chat.isP2p() ? chatMessage.getSender() : null;
            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_HIGH, ChatActivity.class, extras,
                    chat.isP2p() ? Utils.formatAccountFullName(p2pChatPartner) : chat.getTitle(),
                    R.drawable.chat,
                    chat.isP2p() ? getAvatar(context, p2pChatPartner, NotificationType.Chat) : BitmapFactory.decodeResource(context.getResources(), R.drawable.no_photo_group)).
                    setContentText(chatMessage.hasQuotedMessages() && TextUtils.isEmpty(chatMessage.getContent()) ? Html.fromHtml(context.getString(R.string.last_message_quoted, Utils.formatAccountFullName(chatMessage.getSender()))) : chatMessage.getContent()).
                    setColor(ContextCompat.getColor(context, R.color.colorAccent));
            if (withHeadsUp) {
                notificationBuilder.setVibrate(new long[0]);
            }

            Bundle actionExtras = new Bundle();
            actionExtras.putParcelable(ChatActivity.EXTRA_CHAT, Parcels.wrap(chat));
            actionExtras.putBoolean(ChatActivity.EXTRA_SHOW_KEYBOARD, true);
            addAction(context, notificationBuilder, ChatActivity.class, actionExtras, R.drawable.ic_chat_answer, chat.isP2p() ? R.string.notification_reply_message : R.string.notification_write_message);
        } else {
            StringBuilder sb = new StringBuilder();

            int i = 0;
            List<Chat> inviteList = new ArrayList<>();
            for (Chat chat : chats) {
                if (i == 10) {
                    break;
                }
                ChatMessage chatMessage = chat.getLastMessage();
                String content = chatMessage.getContent();

                CharSequence text = null;
                if (chatMessage.isTextMessage()) {
                    text = chatMessage.hasQuotedMessages() && TextUtils.isEmpty(content) ? Html.fromHtml(context.getString(R.string.last_message_quoted, Utils.formatAccountFullName(chatMessage.getSender()))) :
                            context.getString(R.string.chat_message_text, Utils.formatAccountFullName(chatMessage.getSender()), content);
                } else if (chatMessage.isSubjectMessage()) {
                    text = context.getString(R.string.chat_message_text,
                            Utils.formatAccountFullName(chatMessage.getSender()),
                            context.getResources().getString(R.string.chat_has_rename, content));
                } else if (chatMessage.isNotificationMessage()) {
                    if (checkNotification(chatMessage)) {
                        inviteList.add(chat);
                    } else {
                        ChatNotificationData chatNotification = chatMessage.getNotificationData();
                        if (chatNotification != null) {
                            text = Utils.buildChatNotificationMessageText(context, chatMessage.getSender(), chatNotification);
                        }
                    }
                } else if (chatMessage.isContactMessage() && chatMessage.getSharedContact() != null) {
                    text = Utils.buildChatMessageText(context, chatMessage);
                }
                if (text != null) {
                    sb.append(text);
                    sb.append('\n');
                    i++;
                }
            }
            if (!inviteList.isEmpty()) {
                createInvitationNotification(context, inviteList, withHeadsUp && vibrate, withSound);
            }

            int unreadMessagesCount = BusinessLogic.GetInstance().GetNewMessagesCount();

            String title = context.getString(R.string.notification_messages_in_chats,
                    context.getResources().getQuantityString(R.plurals.notification_messages_count, unreadMessagesCount, unreadMessagesCount),
                    context.getResources().getQuantityString(R.plurals.notification_chats_count, chats.size(), chats.size()));
            Bundle extras = new Bundle();
            extras.putBoolean(MainActivity.EXTRA_OPEN_CHATS, true);
            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_DEFAULT, MainActivity.class, extras, title, R.drawable.d_medium, R.mipmap.ic_launcher).
                    setStyle(new NotificationCompat.BigTextStyle().bigText(sb.toString()));
        }

        setupVibrationAndSound(notificationBuilder, withHeadsUp && vibrate, withSound);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CHATS_NOTIFICATION_ID, notificationBuilder.build());
    }

    private static void createInvitationNotification(Context context, List<Chat> chats, boolean vibrate, boolean withSound) {
        NotificationCompat.Builder notificationBuilder;
        if (chats.size() == 1) {
            Chat chat = chats.get(0);
            ChatMessage chatMessage = chat.getLastMessage();

            Bundle extras = new Bundle();
            extras.putParcelable(ChatActivity.EXTRA_CHAT, Parcels.wrap(chat));

            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_LOW, ChatActivity.class, extras, chat.getTitle(), R.drawable.chat, R.drawable.no_photo_group).
                    setContentText(context.getString(R.string.notification_message_invite_full, Utils.formatAccountFullName(chatMessage.getSender()))).
                    setColor(ContextCompat.getColor(context, R.color.colorAccent));

            Bundle actionExtras = new Bundle();
            actionExtras.putParcelable(ChatActivity.EXTRA_CHAT, Parcels.wrap(chat));
            actionExtras.putBoolean(ChatActivity.EXTRA_SHOW_KEYBOARD, true);
            addAction(context, notificationBuilder, ChatActivity.class, actionExtras, R.drawable.ic_chat_answer, R.string.notification_write_message);
        } else {
            List<String> messages = new ArrayList<>();
            for (Chat chat : chats) {
                ChatMessage chatMessage = chat.getLastMessage();
                messages.add(context.getString(R.string.notification_message_invite_full, Utils.formatAccountFullName(chatMessage.getSender())));
            }

            String title = context.getResources().getQuantityString(R.plurals.notification_invitation_to_chats_count, chats.size(), chats.size());
            Bundle extras = new Bundle();
            extras.putBoolean(MainActivity.EXTRA_OPEN_CHATS, true);
            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_DEFAULT, MainActivity.class, extras, title, R.drawable.d_medium, R.mipmap.ic_launcher).
                    setStyle(new NotificationCompat.BigTextStyle().bigText(TextUtils.join("\n", messages)));
        }

        setupVibrationAndSound(notificationBuilder, vibrate, withSound);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CHATS_INVITATION_NOTIFICATION_ID, notificationBuilder.build());
    }

    private static boolean checkNotification(ChatMessage chatMessage) {
        ChatNotificationData cnd = chatMessage.getNotificationData();
        if (cnd != null) {
            if (ChatNotificationData.CHAT_NOTIFICATION_TYPE_INVITE.equals(cnd.getType())) {
                for (Contact contact : cnd.getContacts()) {
                    if (contact.iAm) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void createHistoryNotification(Context context, CallHistoriesList list, boolean vibrate) {
        sPendingAvatarsForNotifications.remove(NotificationType.MissedCall);
        int missedCallsCount = 0;

        for (CallHistory h : list) {
            missedCallsCount += h.statistics.numberOfMissedCalls;
        }

        if (missedCallsCount == 0) {
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_HISTORY, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder;
        if (missedCallsCount == 1) {
            CallHistory history = list.get(0);

            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_LOW, NotificationCompat.CATEGORY_CALL, pendingIntent,
                    history.contact != null ? Utils.formatAccountFullName(history.contact) : history.identity,
                    R.drawable.call_notification_ic,
                    getAvatar(context, history.contact, NotificationType.MissedCall)).
                    setColor(Color.RED).
                    setVibrate(new long[0]).
                    setContentText(context.getString(R.string.missed_call));

            if (history.contact != null) {
                PendingIntent pendingIntentWrite = PendingIntent.getService(context, 0, HandsUpActionsService.newIntentForWrite(context, history.contact), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                PendingIntent pendingIntentCall = PendingIntent.getService(context, 1, HandsUpActionsService.newIntentForCall(context, history.contact), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                addAction(context, notificationBuilder, pendingIntentWrite, R.drawable.ic_chat_answer, R.string.notification_write_message);
                addAction(context, notificationBuilder, pendingIntentCall, R.drawable.ic_call_answer, R.string.notification_new_call);
            } else {
                PendingIntent pendingIntentCall = PendingIntent.getService(context, 1, HandsUpActionsService.newIntentForPstnCall(context, history.identity), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                addAction(context, notificationBuilder, pendingIntentCall, R.drawable.ic_call_answer, R.string.notification_new_call);
            }
        } else {
            String title = context.getResources().getString(R.string.notification_missed_calls_count, missedCallsCount);

            List<String> bigText = new ArrayList<>();

            int i = 0;

            for (CallHistory h : list) {
                if (i == 10) {
                    break;
                }

                if (h.statistics.numberOfMissedCalls > 0) {
                    bigText.add(context.getString(R.string.notification_missed_call_from,
                            h.contact != null
                                    ? Utils.formatAccountFullName(h.contact)
                                    : h.identity));
                    i++;
                }
            }

            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_LOW, NotificationCompat.CATEGORY_CALL, pendingIntent, title, R.drawable.d_medium, R.mipmap.ic_launcher)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(TextUtils.join("\n", bigText)));
        }

        if (notificationBuilder != null) {
            if (vibrate) {
                notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE);
                notificationBuilder.setOnlyAlertOnce(false);
            }
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(HISTORY_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public static void createInviteNotification(Context context, List<Contact> list, boolean vibrate, boolean withSound) {
        sPendingAvatarsForNotifications.remove(NotificationType.Invite);
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder;

        int newInvitationsCount = list.size();

        if (newInvitationsCount == 1) {
            Contact contact = list.get(0);

            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_LOW, NotificationCompat.CATEGORY_SOCIAL, pendingIntent, Utils.formatAccountFullName(contact), R.drawable.invitation, getAvatar(context, contact, NotificationType.Invite)).
                    setColor(Color.RED).
                    setVibrate(new long[0]).
                    setContentText(context.getString(R.string.new_invitation));

            PendingIntent pendingIntentDecline = PendingIntent.getService(context, 2, HandsUpActionsService.newIntentForDeclineInvitation(context, contact), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
            PendingIntent pendingIntentAccept = PendingIntent.getService(context, 3, HandsUpActionsService.newIntentForAcceptInvitation(context, contact), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

            addAction(context, notificationBuilder, pendingIntentDecline, R.drawable.decline_notification_ic, R.string.notification_decline_invitation);
            addAction(context, notificationBuilder, pendingIntentAccept, R.drawable.ic_notification_accept, R.string.notification_accept_invitation);
        } else {
            String title = context.getResources().getQuantityString(R.plurals.notification_invitations_count, newInvitationsCount, newInvitationsCount);

            List<String> bigText = new ArrayList<>();

            int i = 0;

            for (Contact c : list) {
                if (i == 10) {
                    break;
                }

                bigText.add(context.getString(R.string.notification_invitation_you, Utils.formatAccountFullName(c)));
                i++;
            }

            notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_LOW, NotificationCompat.CATEGORY_EVENT, pendingIntent, title, R.drawable.d_medium, R.mipmap.ic_launcher)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(TextUtils.join("\n", bigText)));
        }

        if (notificationBuilder != null) {
            setupVibrationAndSound(notificationBuilder, vibrate, withSound);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(INVITATION_NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    public static void createOutgoingCallNotification(Context context, Call call) {
        sPendingAvatarsForNotifications.remove(NotificationType.Call);

        final Intent intent = new Intent(context.getApplicationContext(), OutgoingCallActivity.class).
                putExtra(CallReceiver.CALL, Parcels.wrap(call));
        final TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context).
                addParentStack(OutgoingCallActivity.class).
                addNextIntent(intent);

        NotificationCompat.Builder notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_MAX, NotificationCompat.CATEGORY_CALL, taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT), Utils.getCallIdentity(call), R.drawable.call_notification_ic, getAvatar(context, call.contact, NotificationType.Call))
                .setContentText(context.getString(R.string.notification_outgoing_call))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setOngoing(true)
                .setAutoCancel(false);

        addAction(context, notificationBuilder, PendingIntent.getBroadcast(context, 0, new Intent(CallReceiver.ACTION_HANGUP_CALL), 0), R.drawable.decline_notification_ic, R.string.notification_end_call);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CALL_NOTIFICATION_ID, notificationBuilder.build());
    }

    public static void createIncomingCallNotification(Context context, Call call, boolean withVibration) {
        sPendingAvatarsForNotifications.remove(NotificationType.Call);

        final Intent intent = new Intent(context.getApplicationContext(), IncomingCallActivity.class).
                putExtra(CallReceiver.CALL, Parcels.wrap(call));

        final TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context).
                addParentStack(IncomingCallActivity.class).
                addNextIntent(intent);
        PendingIntent contentIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_MAX, NotificationCompat.CATEGORY_CALL, contentIntent, Utils.getCallIdentity(call), R.drawable.call_notification_ic, getAvatar(context, call.contact, NotificationType.Call))
                .setContentText(context.getString(R.string.notification_incoming_call))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setOngoing(true)
                .setAutoCancel(false);

        if (withVibration) {
            notificationBuilder.setFullScreenIntent(contentIntent, true);
        }
        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        addAction(context, notificationBuilder, PendingIntent.getBroadcast(context, 0, new Intent(CallReceiver.ACTION_HANGUP_CALL), 0), R.drawable.decline_notification_ic, R.string.notification_decline_call);
        addAction(context, notificationBuilder, PendingIntent.getBroadcast(context, 0, new Intent(CallReceiver.ACTION_ACCEPT_CALL), 0), R.drawable.call_answer_notification_ic, R.string.notification_answer_call);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CALL_NOTIFICATION_ID, notificationBuilder.build());
    }

    public static void createActiveCallNotification(Context context, Call call) {
        sPendingAvatarsForNotifications.remove(NotificationType.Call);

        final Intent intent = new Intent(context.getApplicationContext(), ActiveCallActivity.class).
                putExtra(CallReceiver.CALL, Parcels.wrap(call));

        final TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context).
                addParentStack(ActiveCallActivity.class).
                addNextIntent(intent);

        NotificationCompat.Builder notificationBuilder = createBuilder(context, NotificationCompat.PRIORITY_MAX, NotificationCompat.CATEGORY_CALL, taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT), Utils.getCallIdentity(call), R.drawable.call_notification_ic, getAvatar(context, call.contact, NotificationType.Call))
                .setContentText(context.getString(R.string.notification_active_call))
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setUsesChronometer(true)
                .setOngoing(true)
                .setAutoCancel(false);

        addAction(context, notificationBuilder, PendingIntent.getBroadcast(context, 0, new Intent(CallReceiver.ACTION_HANGUP_CALL), 0), R.drawable.decline_notification_ic, R.string.notification_end_call);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(CALL_NOTIFICATION_ID, notificationBuilder.build());
    }

    private static NotificationCompat.Builder createBuilder(Context context, int priority, Class<? extends Activity> contentActivity, Bundle contentExtras, String title, @DrawableRes int smallIconResId, @DrawableRes int largeIconResId) {
        return createBuilder(context, priority, contentActivity, contentExtras, title, smallIconResId, BitmapFactory.decodeResource(context.getResources(), largeIconResId));
    }

    private static NotificationCompat.Builder createBuilder(Context context, int priority, Class<? extends Activity> contentActivity, Bundle contentExtras, String title, @DrawableRes int smallIconResId, Bitmap largeIcon) {
        Intent intent = new Intent(context, contentActivity);
        intent.putExtras(contentExtras);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        return createBuilder(context, priority, NotificationCompat.CATEGORY_MESSAGE, pendingIntent, title, smallIconResId, largeIcon);
    }

    private static NotificationCompat.Builder createBuilder(Context context, int priority, String category, PendingIntent pendingIntent, String title, @DrawableRes int smallIcon, @DrawableRes int largeIconResId) {
        return createBuilder(context, priority, category, pendingIntent, title, smallIcon, BitmapFactory.decodeResource(context.getResources(), largeIconResId));
    }

    private static NotificationCompat.Builder createBuilder(Context context, int priority, String category, PendingIntent pendingIntent, String title, @DrawableRes int smallIcon, Bitmap largeIcon) {
        return new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setPriority(priority)
                .setCategory(category)
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(pendingIntent)
                .setContentTitle(title);
    }

    private static void addAction(Context context, NotificationCompat.Builder notificationBuilder, Class<? extends Activity> actionActivityClass, Bundle actionExtras, @DrawableRes int actionDrawableResId, @StringRes int actionTitleResId) {
        Intent btnIntent = new Intent(context, actionActivityClass);
        btnIntent.putExtras(actionExtras);
        btnIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        addAction(context, notificationBuilder, PendingIntent.getActivity(context, 1, btnIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT), actionDrawableResId, actionTitleResId);
    }

    private static void addAction(Context context, NotificationCompat.Builder notificationBuilder, PendingIntent pendingIntent, @DrawableRes int actionDrawableResId, @StringRes int actionTitleResId) {
        notificationBuilder.addAction(actionDrawableResId, context.getString(actionTitleResId), pendingIntent);
    }

    private static Bitmap getAvatar(Context context, Contact contact, NotificationType notificationType) {
        if (!(contact == null || contact.dodicallId.isEmpty())) {
            if (!TextUtils.isEmpty(contact.avatarPath)) {
                return ImageUtil.getAvatar(context, contact.avatarPath);
            } else {
                sPendingAvatarsForNotifications.put(notificationType, contact.dodicallId);
            }
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.no_photo_user);
    }

    private static void setupVibrationAndSound(NotificationCompat.Builder notificationBuilder, boolean vibrate, boolean withSound) {
        if (vibrate) {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE);
            notificationBuilder.setOnlyAlertOnce(false);
            if (withSound) {
                notificationBuilder.setSound(INCOMING_MESSAGE_WAV_URI);
            }
        }
    }
}
