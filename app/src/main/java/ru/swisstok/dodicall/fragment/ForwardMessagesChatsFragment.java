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

package ru.swisstok.dodicall.fragment;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;

import java.util.ArrayList;

import ru.swisstok.dodicall.R;

/**
 * @author Roman Radko
 * @since 1.0
 */

public class ForwardMessagesChatsFragment extends ChatsFragment {

    public static final String INITIAL_CHAT_ID = "arg.InitialChatId";

    public static final int TAB_POSITION = 1;

    private String mInitialChatId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInitialChatId = getArguments().getString(INITIAL_CHAT_ID);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contacts_tab, menu);

        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<ChatItem>> loader, ArrayList<ChatItem> data) {
        for (int i = 0; i < data.size(); i++) {
            ChatItem chat = data.get(i);
            if (chat.getChat().getId().equals(mInitialChatId)) {
                data.remove(i);
                data.add(0, chat);
                break;
            }
        }

        super.onLoadFinished(loader, data);
    }
}
