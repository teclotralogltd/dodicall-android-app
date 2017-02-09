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
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.DataProviderHelper;

public class CreateChatAsyncTask extends ContentResolverAsyncTask<Chat> {
    public interface OnCreateChatListener {
        void onChatCreated(Chat chat);
    }

    private final ArrayList<Contact> mContacts;
    private final WeakReference<OnCreateChatListener> mListenerRef;

    public CreateChatAsyncTask(@NonNull Context context, @NonNull ArrayList<Contact> contacts, @NonNull OnCreateChatListener listener) {
        super(context);
        mContacts = contacts;
        mListenerRef = new WeakReference<>(listener);
    }

    @Nullable
    @Override
    public Chat loadInBackground(@NonNull ContentResolver contentResolver) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mContacts.size(); ++i) {
            sb.append(mContacts.get(i).id);

            if (i < mContacts.size() - 1) {
                sb.append(',');
            }
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put("contactIds", sb.toString());

        Uri uri = contentResolver.insert(DataProvider.makeChatsUri(), contentValues);

        if (uri == null) {
            return null;
        }

        return DataProviderHelper.getChat(contentResolver, uri);
    }

    @Override
    protected void onPostExecute(Chat result) {
        OnCreateChatListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onChatCreated(result);
        }
    }

    public static void execute(@NonNull Context context, @NonNull ArrayList<Contact> contacts, @NonNull OnCreateChatListener listener) {
        new CreateChatAsyncTask(context, contacts, listener).execute();
    }

    public static void execute(@NonNull Context context, @NonNull Contact contact, @NonNull OnCreateChatListener listener) {
        ArrayList<Contact> contacts = new ArrayList<>();
        contacts.add(contact);

        new CreateChatAsyncTask(context, contacts, listener).execute();
    }
}
