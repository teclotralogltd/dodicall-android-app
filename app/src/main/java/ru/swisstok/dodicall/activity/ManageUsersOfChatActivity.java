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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.InviteAndRevokeChatMembersAsyncTask;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.DataProviderHelper;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.SimpleDividerItemDecoration;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class ManageUsersOfChatActivity extends BaseActivity implements InviteAndRevokeChatMembersAsyncTask.OnInviteAndRevokeChatMembersListener {
    public static final String EXTRA_CHAT = "extra_chat";

    private static final int REQUEST_CODE_SELECT_ACCOUNTS = 200;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.chat_sub_title_text)
    TextView mChatSubTitleText;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private ProgressDialog mProgressDialog;

    private Chat mChat;
    private ArrayList<Contact> mContacts;
    private boolean mDeletingMode = false;
    private Contact[] mSelected;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (TextUtils.equals(action, DataProvider.ACTION_CHATS_RECREATED)) {
                String[] ids = DataProvider.extractExtraIds(intent);

                if (ids != null) {
                    if (mChat.getId().equals(ids[0])) {
                        mChat = DataProviderHelper.getChat(getContentResolver(), ids[1]);
                        setupActionBar();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users_of_chat);
        ButterKnife.bind(this);

        mChat = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CHAT));

        if (CollectionUtils.isEmpty(mChat.getContacts())) {
            Toast.makeText(this, "ERROR: Contacts list null or empty!!!!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupActionBar();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mRecyclerView.setAdapter(new Adapter());

        changeMode(false);

        updateContacts(Stream.of(mChat.getContacts()).filter(contact -> !contact.iAm).collect(Collectors.toCollection(ArrayList<Contact>::new)));

        LocalBroadcast.registerReceiver(this, mBroadcastReceiver, DataProvider.ACTION_CHATS_RECREATED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcast.unregisterReceiver(this, mBroadcastReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_manage_users_of_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_users_chat) {
            startActivityForResult(SelectContactsActivity.newIntent(this, mContacts, true), REQUEST_CODE_SELECT_ACCOUNTS);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);
        setTitle(null);

        updateChatInfo();
    }

    private void updateChatInfo() {
        mChatSubTitleText.setText((!mChat.isP2p() && mChat.getTitle().length() < 30)
                ? mChat.getTitle()
                : null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_ACCOUNTS) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Contact> selectedContacts = SelectContactsActivity.extractResult(data);
                if (CollectionUtils.isNotEmpty(selectedContacts)) {
                    updateContacts(selectedContacts);
                    inviteAndRevokeChatMembers();
                }
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onInvitedAndRevokedChatMembers(Chat chat) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    void inviteAndRevokeChatMembers() {
        mProgressDialog = Utils.showProgress(this, R.string.progress_message_update_members_chat);

        ArrayList<Contact> inviteContacts = new ArrayList<>();

        for (Contact c : mContacts) {
            Iterator<Contact> it = mChat.getContacts().iterator();
            boolean found = false;
            while (it.hasNext()) {
                if (c.id == it.next().id) {
                    it.remove();
                    found = true;
                    break;
                }
            }

            if (!found) {
                inviteContacts.add(c);
            }
        }

        ArrayList<Contact> revokeContacts = mChat.getContacts();
        InviteAndRevokeChatMembersAsyncTask.execute(this, mChat.getId(), inviteContacts, revokeContacts, this);
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.select_check_box)
        CheckBox selectCheckBox;

        @BindView(R.id.contact_avatar)
        RoundedImageView contactAvatar;

        @BindView(R.id.name)
        TextView nameText;

        @BindView(R.id.status_text)
        TextView statusText;

        public ContactViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class Adapter extends RecyclerView.Adapter<ContactViewHolder> {

        @Override
        public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.contact_item, parent, false);
            return new ContactViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ContactViewHolder holder, final int position) {
            final Contact item = mContacts.get(position);

            holder.selectCheckBox.setVisibility(mDeletingMode
                    ? View.VISIBLE
                    : View.GONE);

            holder.nameText.setText(Utils.formatAccountFullName(item));
            holder.contactAvatar.setUrl(item.avatarPath);
            if(item.isMine) {
                Utils.setVisibilityVisible(holder.statusText);
                StatusesAdapter.setupStatusView(holder.statusText, item.getStatus(), item.getExtraStatus());
            }else{
                Utils.setVisibilityGone(holder.statusText);
            }

            holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mSelected[position] = isChecked ? item : null;
                updateSelectedContactsTitle();
            });

            holder.selectCheckBox.setChecked(mSelected[position] != null);

            if (mDeletingMode) {
                holder.itemView.setOnClickListener(v -> holder.selectCheckBox.setChecked(mSelected[position] == null));
            } else {
                holder.itemView.setOnClickListener(v -> ProfileActivity.openProfile(ManageUsersOfChatActivity.this, item));
            }

            holder.itemView.setOnLongClickListener(v -> {
                        if (!mDeletingMode) {
                            Arrays.fill(mSelected, null);
                            mSelected[position] = item;
                            changeMode(!mChat.isP2p());
                            updateSelectedContactsTitle();
                            return true;
                        }

                        return false;
                    }
            );
        }

        @Override
        public int getItemCount() {
            return mContacts == null ? 0 : mContacts.size();
        }

    }

    private ActionMode mActionMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            getMenuInflater().inflate(R.menu.delete_chat_members_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();

            if (id == R.id.action_select_all) {
                selectAll(true);
                return true;
            } else if (id == R.id.action_deselect_all) {
                selectAll(false);
                return true;
            } else if (id == R.id.action_delete_items) {
                deleteSelectedContacts();
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            changeMode(false);
        }
    };

    private void deleteSelectedContacts() {
        int deletedContactsCount = 0;
        ArrayList<Contact> remainedContacts = new ArrayList<>();

        for (int i = 0; i < mContacts.size(); i++) {
            if (mSelected[i] == null) {
                remainedContacts.add(mContacts.get(i));
            } else {
                deletedContactsCount++;
            }
        }

        if (deletedContactsCount > 0) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.alert_title_delete_contacts)
                    .setMessage(R.string.alert_dialog_message_delete_contacts_confirm)
                    .setPositiveButton(R.string.yes_delete_chats, (dialog, which) -> {
                        updateContacts(remainedContacts);

                        if (mActionMode != null) {
                            mActionMode.finish();
                        }

                        changeMode(false);

                        inviteAndRevokeChatMembers();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void selectAll(boolean select) {

        if (!select) {
            Arrays.fill(mSelected, null);
            updateSelectedContactsTitle(0);
        } else {
            for (int i = 0; i < mContacts.size(); i++) {
                mSelected[i] = mContacts.get(i);
            }

            updateSelectedContactsTitle(mSelected.length);
        }

        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void changeMode(boolean deletingMode) {
        mDeletingMode = deletingMode;

        if (mDeletingMode) {
            mToolbar.startActionMode(mActionModeCallback);
        } else if (mActionMode != null) {
            mActionMode.finish();
        }

        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public static Intent newIntent(Context context, @NonNull Chat chat) {
        return new Intent(context, ManageUsersOfChatActivity.class)
                .putExtra(EXTRA_CHAT, Parcels.wrap(chat));
    }

    private void updateContacts(ArrayList<Contact> newContacts) {
        mContacts = newContacts;
        mSelected = new Contact[newContacts.size()];
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    private void updateSelectedContactsTitle() {
        if (mActionMode != null) {
            int count = 0;
            for (Contact c : mSelected) {
                if (c != null) {
                    count++;
                }
            }

            updateSelectedContactsTitle(count);
        }
    }

    private void updateSelectedContactsTitle(int count) {
        if (mActionMode != null) {
            mActionMode.setTitle(getResources().getString(R.string.toolbar_selected_contacts_count, count));
        }
    }
}
