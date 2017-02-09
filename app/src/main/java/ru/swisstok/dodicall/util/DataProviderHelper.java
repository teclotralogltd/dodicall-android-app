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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.provider.DataProvider;

public class DataProviderHelper {

    private static final Decoder<Contact> CONTACT_DECODER = (cr, cursor) -> new Contact(cursor);

    private static final Decoder<ArrayList<Contact>> CONTACTS_DECODER = (cr, cursor) -> {
        ArrayList<Contact> contacts = new ArrayList<>(cursor.getCount());

        do {
            contacts.add(CONTACT_DECODER.decode(cr, cursor));
        } while (cursor.moveToNext());

        return contacts;
    };

    private static final Decoder<ChatNotificationData> CHAT_NOTIFICATION_DATA_DECODER = (cr, cursor) -> {
        String type = CursorHelper.getString(cursor, ChatNotificationData.COLUMN_TYPE);
        String chatMessageId = CursorHelper.getString(cursor, ChatNotificationData.COLUMN_CHAT_MESSAGE_ID);
        Logger.onOperationStart("GetMessageNotificationContacts");
        ArrayList<Contact> contacts = query(cr, DataProvider.makeMessageNotificationDataContactsUri(chatMessageId), CONTACTS_DECODER);
        Logger.onOperationEnd("GetMessageNotificationContacts");

        return new ChatNotificationData(type, contacts);
    };

    private static final Decoder<ChatMessage> CHAT_MESSAGE_DECODER = (cr, cursor) -> {
        String _id = CursorHelper.getString(cursor, ChatMessage.COLUMN_ID);
        String _chatId = CursorHelper.getString(cursor, ChatMessage.COLUMN_CHAT_ID);
        boolean _servered = CursorHelper.getBoolean(cursor, ChatMessage.COLUMN_SERVERED);
        long _sendTime = CursorHelper.getLong(cursor, ChatMessage.COLUMN_SEND_TIME);
        boolean _read = CursorHelper.getBoolean(cursor, ChatMessage.COLUMN_READ);
        boolean _edited = CursorHelper.getBoolean(cursor, ChatMessage.COLUMN_EDITED);
        String _content = CursorHelper.getString(cursor, ChatMessage.COLUMN_CONTENT);
        String _type = CursorHelper.getString(cursor, ChatMessage.COLUMN_TYPE);
        int _rownum = CursorHelper.getInt(cursor, ChatMessage.COLUMN_ROWNUM);
        boolean _encrypted = CursorHelper.getBoolean(cursor, ChatMessage.COLUMN_ENCRYPTED);

        Logger.onOperationStart("GetMessageContact");
        Contact _sender = query(cr, DataProvider.makeMessageSenderUri(_chatId), CONTACT_DECODER);
        Logger.onOperationEnd("GetMessageContact");

        ChatNotificationData _cnd = null;
        if (_type.equals(ChatMessage.TYPE_NOTIFICATION)) {
            Logger.onOperationStart("GetMessageNotification");
            _cnd = query(cr, DataProvider.makeMessageNotificationDataUri(_chatId), CHAT_NOTIFICATION_DATA_DECODER);
            Logger.onOperationEnd("GetMessageNotification");
        }

        return new ChatMessage(_id, _chatId, _servered, _sendTime, _read, _edited, _content, _type, _sender, _cnd, _rownum, _encrypted, null);
    };

    private static final Decoder<Chat> CHAT_DECODER = (cr, cursor) -> {
        String _id = CursorHelper.getString(cursor, Chat.COLUMN_ID);
        String _customTitle = CursorHelper.getString(cursor, Chat.COLUMN_CUSTOM_TITLE);
        boolean _active = CursorHelper.getBoolean(cursor, Chat.COLUMN_ACTIVE);
        int _totalMessagesCount = CursorHelper.getInt(cursor, Chat.COLUMN_TOTAL_MESSAGES_COUNT);
        int _newMessagesCount = CursorHelper.getInt(cursor, Chat.COLUMN_NEW_MESSAGES_COUNT);
        long _lastModifiedDate = CursorHelper.getLong(cursor, Chat.COLUMN_LAST_MODIFIED_DATE);
        boolean _p2p = CursorHelper.getBoolean(cursor, Chat.COLUMN_IS_P2P);
        Logger.onOperationStart("GetChatLastMessage");
        ChatMessage _lastMessage = query(cr, DataProvider.makeChatMessagesLastUri(_id), CHAT_MESSAGE_DECODER);
        Logger.onOperationEnd("GetChatLastMessage");
        Logger.onOperationStart("GetChatContacts");
        ArrayList<Contact> _contacts = query(cr, DataProvider.makeChatContactsUri(_id), CONTACTS_DECODER);
        Logger.onOperationEnd("GetChatContacts", _customTitle);
        String incompleteMessage = CursorHelper.getString(cursor, Chat.COLUMN_INCOMPLETE_MESSAGE);

        return new Chat(_id, _customTitle, _active, _totalMessagesCount, _newMessagesCount, _lastModifiedDate, _p2p, _lastMessage, _contacts, incompleteMessage);
    };

    private static final Decoder<ArrayList<Chat>> CHATS_DECODER = (cr, cursor) -> {
        ArrayList<Chat> chats = new ArrayList<>(cursor.getCount());

        do {
            Logger.onOperationStart("DecodeChat");
            chats.add(CHAT_DECODER.decode(cr, cursor));
            Logger.onOperationEnd("DecodeChat");
        } while (cursor.moveToNext());

        return chats;
    };

    @Nullable
    public static ArrayList<Chat> getChats(@NonNull ContentResolver cr) {
        return query(cr, DataProvider.makeChatsUri(), CHATS_DECODER);
    }

    @Nullable
    public static Chat getChat(@NonNull ContentResolver cr, @NonNull String chatId) {
        return getChat(cr, DataProvider.makeChatUri(chatId));
    }

    @Nullable
    public static Chat getChat(@NonNull ContentResolver cr, @NonNull Uri chatUri) {
        return query(cr, chatUri, CHAT_DECODER);
    }

    @Nullable
    private static <T> T query(@NonNull ContentResolver cr, @NonNull Uri uri, @NonNull Decoder<T> decoder) {
        return query(cr, _cr -> _cr.query(uri, null, null, null, null), decoder);
    }

    @Nullable
    private static <T> T query(@NonNull ContentResolver cr, @NonNull Requester requester, @NonNull Decoder<T> decoder) {
        Cursor cursor = requester.query(cr);

        if (cursor == null) {
            return null;
        }

        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        try {
            return decoder.decode(cr, cursor);
        } finally {
            cursor.close();
        }
    }

    private interface Requester {
        @Nullable
        Cursor query(@NonNull ContentResolver cr);
    }

    private interface Decoder<T> {
        @Nullable
        T decode(@NonNull ContentResolver cr, @NonNull Cursor cursor);
    }

    private DataProviderHelper() {
    }
}