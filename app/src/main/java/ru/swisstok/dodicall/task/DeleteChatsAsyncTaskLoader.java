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
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.StringList;

public class DeleteChatsAsyncTaskLoader extends ContentResolverAsyncTaskLoader<ArrayList<Chat>> {
    private final ArrayList<Chat> mChats;

    public DeleteChatsAsyncTaskLoader(Context context, @NonNull ArrayList<Chat> chats) {
        super(context);
        mChats = chats;
    }

    @Nullable
    @Override
    public ArrayList<Chat> loadInBackground(@NonNull ContentResolver contentResolver) {
        StringList ids = new StringList();
        StringList failed = new StringList();


        for (Chat chat : mChats) {
            ids.add(chat.getId());
        }

        ArrayList<Chat> deleted = new ArrayList<>();
        ArrayList<String> deletedIds = new ArrayList<>();

        if (BusinessLogic.GetInstance().ClearChats(ids, failed)) {
            for (Chat chat : mChats) {
                String id = chat.getId();
                boolean success = true;

                for (int j = 0; j < failed.size(); ++j) {
                    if (failed.get(j).equals(id)) {
                        success = false;
                        break;
                    }
                }

                if (success) {
                    int r = contentResolver.delete(DataProvider.makeChatUri(id), null, null);
                    if (r == 1) {
                        deleted.add(chat);
                        deletedIds.add(chat.getId());
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(deletedIds)) {
            DataProvider.sendLocalBroadcastForIds(getContext(), DataProvider.ACTION_CHATS_DELETED, deletedIds);
        }

        return deleted;
    }
}
