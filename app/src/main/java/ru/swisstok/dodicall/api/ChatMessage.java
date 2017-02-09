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

import android.content.ContentValues;

import org.parceler.Parcel;

import java.util.List;

import ru.uls_global.dodicall.ChatMessageType;

@Parcel
public class ChatMessage implements DbStruct {

    public enum MessageAction {
        Reply, Quote, Forward
    }

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CHAT_ID = "chat_id";
    public static final String COLUMN_SERVERED = "servered";
    public static final String COLUMN_SEND_TIME = "send_time";
    public static final String COLUMN_READ = "is_read";
    public static final String COLUMN_EDITED = "is_edited";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_ROWNUM = "rownum";
    public static final String COLUMN_ENCRYPTED = "encrypted";

    public final static String TYPE_TEXT_MESSAGE = ChatMessageType.ChatMessageTypeTextMessage.toString();
    public final static String TYPE_SUBJECT = ChatMessageType.ChatMessageTypeSubject.toString();
    public final static String TYPE_AUDIO_MESSAGE = ChatMessageType.ChatMessageTypeAudioMessage.toString();
    public final static String TYPE_NOTIFICATION = ChatMessageType.ChatMessageTypeNotification.toString();
    public final static String TYPE_CONTACT = ChatMessageType.ChatMessageTypeContact.toString();
    public final static String TYPE_DELETER = ChatMessageType.ChatMessageTypeDeleter.toString();
    public final static String TYPE_QUOTED = "ChatMessageTypeQuoted";

    String mId;
    String mChatId;
    boolean mServered;
    long mSendTime;
    boolean mRead;
    boolean mEdited;
    String mContent;
    String mType;
    Contact mSender;
    ChatNotificationData mNotificationData;
    int mRownum;
    boolean mEncrypted;
    Contact mSharedContact;
    List<ChatMessage> mQuotedMessages;

    public ChatMessage(String id, String chatId, boolean servered, long sendTime, boolean read, boolean edited, String content, String type, Contact sender, ChatNotificationData notificationData, int rownum, boolean encrypted, Contact sharedContact) {
        mId = id;
        mChatId = chatId;
        mServered = servered;
        mSendTime = sendTime;
        mRead = read;
        mEdited = edited;
        mContent = content;
        mType = type;
        mSender = sender;
        mNotificationData = notificationData;
        mRownum = rownum;
        mEncrypted = encrypted;
        mSharedContact = sharedContact;
    }

    public ChatMessage() {

    }

    public String getId() {
        return mId;
    }

    public String getChatId() {
        return mChatId;
    }

    public boolean isServered() {
        return mServered;
    }

    public long getSendTime() {
        return mSendTime;
    }

    public boolean isRead() {
        return mRead;
    }

    public boolean isEdited() {
        return mEdited;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getType() {
        return mType;
    }

    public Contact getSender() {
        return mSender;
    }

    public void setSender(Contact sender) {
        mSender = sender;
    }

    public ChatNotificationData getNotificationData() {
        return mNotificationData;
    }

    public int getRownum() {
        return mRownum;
    }

    public Contact getSharedContact() {
        return mSharedContact;
    }

    public List<ChatMessage> getQuotedMessages() {
        return mQuotedMessages;
    }

    public void setQuotedMessages(List<ChatMessage> quotedMessages) {
        mQuotedMessages = quotedMessages;
    }

    public void setSharedContact(Contact sharedContact) {
        mSharedContact = sharedContact;
    }

    public boolean isTextMessage() {
        return TYPE_TEXT_MESSAGE.equals(mType);
    }

    public boolean isDeletedMessage() {
        return TYPE_DELETER.equals(mType);
    }

    public boolean isNotificationMessage() {
        return TYPE_NOTIFICATION.equals(mType);
    }

    public boolean isSubjectMessage() {
        return TYPE_SUBJECT.equals(mType);
    }

    public boolean isContactMessage() {
        return TYPE_CONTACT.equals(mType);
    }

    public boolean isQuoteMessage() {
        return TYPE_QUOTED.equals(mType);
    }

    public boolean hasQuotedMessages() {
        return mQuotedMessages != null && !mQuotedMessages.isEmpty();
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, mId);
        cv.put(COLUMN_CHAT_ID, mChatId);
        cv.put(COLUMN_SERVERED, mServered);
        cv.put(COLUMN_SEND_TIME, mSendTime);
        cv.put(COLUMN_READ, mRead);
        cv.put(COLUMN_EDITED, mEdited);
        cv.put(COLUMN_CONTENT, mContent);
        cv.put(COLUMN_TYPE, mType);

        return cv;
    }

    public void setRead(boolean read) {
        mRead = read;
    }

    public boolean isEncrypted() {
        return mEncrypted;
    }

    public void setEncrypted(boolean encrypted) {
        mEncrypted = encrypted;
    }

}
