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

package ru.swisstok.dodicall.activity;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.TabAdapter;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ContactsFragment;
import ru.swisstok.dodicall.fragment.ForwardMessagesChatsFragment;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.util.NotificationsUtils;

public class ForwardMessagesActivity extends MainActivity {

    public static final String EXTRA_FORWARDED_MESSAGES_LIST = "extra.ForwardedMessages";
    public static final String EXTRA_INITIAL_CHAT_ID = "extra.InitialChatId";

    private List<TabAdapter.TabSpec> mTabs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle contactsBundle = new Bundle(3);
        contactsBundle.putBoolean(ContactsFragment.BUTTONS_CONTACTS_LIST, false);
        contactsBundle.putBoolean(ContactsFragment.REQUESTS_CONTACTS_LIST, false);
        contactsBundle.putBoolean(ContactsFragment.INVITES_CONTACTS_LIST, false);

        mTabs.add(new TabAdapter.TabSpec(
                ContactsFragment.class, R.string.tab_title_contacts,
                R.drawable.contacts_tab_ic, contactsBundle,
                ContactsFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.WithoutNotification
        ));

        Bundle chatBundle = new Bundle(1);
        chatBundle.putString(ForwardMessagesChatsFragment.INITIAL_CHAT_ID, getIntent().getStringExtra(EXTRA_INITIAL_CHAT_ID));

        mTabs.add(new TabAdapter.TabSpec(
                ForwardMessagesChatsFragment.class, R.string.tab_title_chats,
                R.drawable.chats_tab_ic, chatBundle,
                ForwardMessagesChatsFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.Chat
        ));

        getIntent().putExtra(EXTRA_OPEN_CHATS, true);
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        ((Toolbar) findViewById(R.id.toolbar)).setTitle(R.string.title_activity_forward_messages);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public List<TabAdapter.TabSpec> getTabs() {
        return mTabs;
    }

    @Override
    public boolean withContactsSpinner() {
        return false;
    }

    @Override
    public boolean canCreateContacts() {
        return false;
    }

    @Override
    public int getCurrentFilter() {
        return ToolBarSpinnerAdapter.FILTER_DDC;
    }

    @Override
    protected void setupDrawer(Toolbar toolbar) {
    }

    @Override
    protected int getChatTabIndex() {
        return ForwardMessagesChatsFragment.TAB_POSITION;
    }

    @Override
    protected TabLayout.Tab getChatTab() {
        return getTabAt(ForwardMessagesChatsFragment.TAB_POSITION);
    }

    @Override
    protected boolean isShowCounters() {
        return false;
    }

    @Override
    public void onOpenContact(Contact contact) {
        CreateChatAsyncTask.execute(this, contact, chat -> {
            if (chat == null) {
                Toast.makeText(this, R.string.unable_create_chat, Toast.LENGTH_SHORT).show();
                return;
            }

            forwardMessages(chat);
        });
    }

    @Override
    public void onOpenChat(Chat chat) {
        forwardMessages(chat);
    }

    private void forwardMessages(Chat chat) {
        List<ChatMessage> forwardedMessages = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_FORWARDED_MESSAGES_LIST));
        startActivity(ChatActivity.newIntent(this, chat, forwardedMessages));
        finish();
    }
}
