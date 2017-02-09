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

import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.bl.BL;

public class SendChatMessageAsyncTask extends AsyncTask<Void, Void, String> {

    public interface OnSendChatMessageListener {
        void onChatMessageSent(String id);
    }

    private ChatMessage mChatMessage;
    private ChatMessage.MessageAction mMessageAction;
    private final WeakReference<OnSendChatMessageListener> mListenerRef;

    private SendChatMessageAsyncTask(ChatMessage chatMessage, ChatMessage.MessageAction messageAction, OnSendChatMessageListener listener) {
        mChatMessage = chatMessage;
        mMessageAction = messageAction;
        mListenerRef = new WeakReference<>(listener);
    }

    @Override
    protected String doInBackground(Void... params) {
        return BL.sendChatMessage(mChatMessage, mMessageAction);
    }

    @Override
    protected void onPostExecute(String result) {
        OnSendChatMessageListener listener = mListenerRef.get();
        if (listener != null) {
            listener.onChatMessageSent(result);
        }
    }

    public static void execute(ChatMessage chatMessage, ChatMessage.MessageAction messageAction, @NonNull OnSendChatMessageListener listener) {
        new SendChatMessageAsyncTask(chatMessage, messageAction, listener).execute();
    }
}
