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

package ru.swisstok.dodicall.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.DataProviderHelper;

public class InviteAndRevokeChatMembersAsyncTask extends ContentResolverAsyncTask<Chat> {
    public interface OnInviteAndRevokeChatMembersListener {
        void onInvitedAndRevokedChatMembers(Chat chat);
    }

    private static final CollectionUtils.Joiner<Contact> CONTACT_ID_JOINER = value -> String.valueOf(value.id);

    private final String mChatId;
    private final ArrayList<Contact> mInvites;
    private final ArrayList<Contact> mRevokes;
    private final WeakReference<OnInviteAndRevokeChatMembersListener> mListenerRef;

    public InviteAndRevokeChatMembersAsyncTask(@NonNull Context context, String chatId, ArrayList<Contact> revokes, ArrayList<Contact> invites, OnInviteAndRevokeChatMembersListener listenerRef) {
        super(context);
        mChatId = chatId;
        mInvites = revokes;
        mRevokes = invites;
        mListenerRef = new WeakReference<>(listenerRef);
    }

    @Nullable
    @Override
    public Chat loadInBackground(@NonNull ContentResolver contentResolver) {
        ContentValues contentValues = new ContentValues();

        if (CollectionUtils.isNotEmpty(mInvites)) {
            contentValues.put("inviteIds", CollectionUtils.join(mInvites, CONTACT_ID_JOINER, ','));
        }

        if (CollectionUtils.isNotEmpty(mRevokes)) {
            contentValues.put("revokeIds", CollectionUtils.join(mRevokes, CONTACT_ID_JOINER, ','));
        }

        int r = contentResolver.update(DataProvider.makeChatUri(mChatId), contentValues, DataProvider.SELECTION_INVITE_AND_REVOKE_CHAT_MEMBERS, null);

        if (r == 0) {
            return null;
        }

        return DataProviderHelper.getChat(contentResolver, mChatId);
    }

    @Override
    protected void onPostExecute(Chat result) {
        OnInviteAndRevokeChatMembersListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onInvitedAndRevokedChatMembers(result);
        }
    }

    public static void execute(@NonNull Context context, @NonNull String chatId, @NonNull ArrayList<Contact> invites, @NonNull ArrayList<Contact> revokes, @NonNull OnInviteAndRevokeChatMembersListener listener) {
        new InviteAndRevokeChatMembersAsyncTask(context, chatId, invites, revokes, listener).execute();
    }
}
