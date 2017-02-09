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

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import ru.uls_global.dodicall.BusinessLogic;

public class RenameChatAsyncTask extends AsyncTask<Void, Void, Boolean> {
    public interface OnChatRenamedListener {
        void onChatRenamed(boolean result, String subject);
    }

    private final String mChatId;
    private final String mChatSubject;
    private final WeakReference<OnChatRenamedListener> mListenerRef;

    public RenameChatAsyncTask(String chatId, String chatSubject, OnChatRenamedListener listener) {
        mChatId = chatId;
        mChatSubject = chatSubject;
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return BusinessLogic.GetInstance().RenameChat(mChatId, mChatSubject);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        OnChatRenamedListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onChatRenamed(result, mChatSubject);
        }
    }

    public static void execute(String chatId, String newChatSubject, @NonNull OnChatRenamedListener listener) {
        new RenameChatAsyncTask(chatId, newChatSubject, listener).execute();
    }
}
