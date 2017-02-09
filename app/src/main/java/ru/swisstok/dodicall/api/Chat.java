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

import java.util.ArrayList;

@Parcel
public class Chat implements DbStruct {
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CUSTOM_TITLE = "custom_title";
    public static final String COLUMN_ACTIVE = "is_active";
    public static final String COLUMN_TOTAL_MESSAGES_COUNT = "total_messages_count";
    public static final String COLUMN_NEW_MESSAGES_COUNT = "new_messages_count";
    public static final String COLUMN_LAST_MODIFIED_DATE = "last_modified_date";
    public static final String COLUMN_IS_P2P = "is_p2p";
    public static final String COLUMN_INCOMPLETE_MESSAGE = "incomplete_message";

    String mId;
    String mCustomTitle;
    boolean mActive;
    int mTotalMessagesCount;
    int mNewMessagesCount;
    long mLastModifiedDate;
    boolean mP2p;
    ChatMessage mLastMessage;
    ArrayList<Contact> mContacts;
    String mIncompleteMessage;

    public Chat(String id, String customTitle, boolean active, int totalMessagesCount, int newMessagesCount, long lastModifiedDate, boolean p2p, ChatMessage lastMessage, ArrayList<Contact> contacts, String incompleteMessage) {
        mId = id;
        mCustomTitle = customTitle;
        mActive = active;
        mTotalMessagesCount = totalMessagesCount;
        mNewMessagesCount = newMessagesCount;
        mLastModifiedDate = lastModifiedDate;
        mP2p = p2p;
        mLastMessage = lastMessage;
        mContacts = contacts;
        mIncompleteMessage = incompleteMessage;
    }

    public Chat() {
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mCustomTitle;
    }

    public boolean isActive() {
        return mActive;
    }

    public int getTotalMessagesCount() {
        return mTotalMessagesCount;
    }

    public int getNewMessagesCount() {
        return mNewMessagesCount;
    }

    public long getLastModifiedDate() {
        return mLastModifiedDate;
    }

    public boolean isP2p() {
        return mP2p;
    }

    public ChatMessage getLastMessage() {
        return mLastMessage;
    }

    public ArrayList<Contact> getContacts() {
        return mContacts;
    }

    public String getIncompleteMessage() {
        return mIncompleteMessage;
    }

    public void setIncompleteMessage(String incompleteMessage) {
        mIncompleteMessage = incompleteMessage;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, mId);
        cv.put(COLUMN_CUSTOM_TITLE, mCustomTitle);
        cv.put(COLUMN_ACTIVE, mActive ? 1 : 0);
        cv.put(COLUMN_TOTAL_MESSAGES_COUNT, mTotalMessagesCount);
        cv.put(COLUMN_NEW_MESSAGES_COUNT, mNewMessagesCount);
        cv.put(COLUMN_LAST_MODIFIED_DATE, mLastModifiedDate);
        cv.put(COLUMN_IS_P2P, mP2p ? 1 : 0);
        return cv;
    }
}
