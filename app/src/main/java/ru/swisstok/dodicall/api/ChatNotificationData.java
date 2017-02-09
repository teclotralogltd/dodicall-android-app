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

package ru.swisstok.dodicall.api;

import org.parceler.Parcel;

import java.util.ArrayList;

import ru.uls_global.dodicall.ChatNotificationType;

@Parcel
public class ChatNotificationData {
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_CHAT_MESSAGE_ID = "chat_message_id";

    public final static String CHAT_NOTIFICATION_TYPE_CREATE = ChatNotificationType.ChatNotificationTypeCreate.toString();
    public final static String CHAT_NOTIFICATION_TYPE_INVITE = ChatNotificationType.ChatNotificationTypeInvite.toString();
    public final static String CHAT_NOTIFICATION_TYPE_REVOKE = ChatNotificationType.ChatNotificationTypeRevoke.toString();
    public final static String CHAT_NOTIFICATION_TYPE_LEAVE = ChatNotificationType.ChatNotificationTypeLeave.toString();
    public final static String CHAT_NOTIFICATION_TYPE_REMOVE = ChatNotificationType.ChatNotificationTypeRemove.toString();

    String mType;
    ArrayList<Contact> mContacts;

    public ChatNotificationData(String type, ArrayList<Contact> contacts) {
        mType = type;
        mContacts = contacts;
    }

    public ChatNotificationData() {
    }

    public String getType() {
        return mType;
    }

    public ArrayList<Contact> getContacts() {
        return mContacts;
    }
}
