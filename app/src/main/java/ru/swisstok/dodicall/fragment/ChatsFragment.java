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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.SelectContactsActivity;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.ContentResolverAsyncTaskLoader;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.task.DeleteChatsAsyncTaskLoader;
import ru.swisstok.dodicall.util.ChatComparator;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.DataProviderHelper;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Logger;
import ru.swisstok.dodicall.util.OnBackPressedListener;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class ChatsFragment extends BaseTabFragment implements LoaderManager.LoaderCallbacks<ArrayList<ChatsFragment.ChatItem>>, CreateChatAsyncTask.OnCreateChatListener, OnBackPressedListener, SearchView.OnQueryTextListener {

    public static final int TAB_POSITION = 2;

    public static final int LOADER_ID_GET_CHATS = 1001;
    public static final int LOADER_ID_DELETE_CHATS = 1002;

    private static final String CHATS_EXTRA = "chats";
    private static final int REQUEST_CODE_SELECT_ACCOUNTS = 200;
    private static final ChatComparator CHAT_COMPARATOR = new ChatComparator();

    @BindView(R.id.no_data_text)
    TextView mNoDataText;

    @BindView(R.id.progress)
    ProgressBar mProgress;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    ProgressDialog mProgressDialog;

    private ArrayList<ChatItem> mChatItems = new ArrayList<>();
    private boolean mStateDeleting;
    private int mSelectedChats;
    private ActionMode mActionMode;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, DataProvider.ACTION_CHATS_UPDATED)
                    || TextUtils.equals(action, DataProvider.ACTION_CHATS_DELETED)
                    || TextUtils.equals(action, ContactsManager.ACTION_USERS_STATUSES_UPDATED)
                    || TextUtils.equals(action, BaseManager.AVATAR_LOADED)) {
                if (isAdded()) {
                    getLoaderManager().initLoader(LOADER_ID_GET_CHATS, null, ChatsFragment.this);
                }
            }
        }
    };

    private void selectAll(boolean select) {
        setSelectedChats(select ? mChatItems.size() : 0);

        for (ChatItem item : mChatItems) {
            item.setDeleting(select);
        }

        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onBackPressed() {
        if (mStateDeleting) {
            changeState(false);
            return true;
        }

        return false;
    }

    private LoaderManager.LoaderCallbacks<ArrayList<Chat>> mDeleteChatsLoaderCallbacks = new LoaderManager.LoaderCallbacks<ArrayList<Chat>>() {

        @Override
        public Loader<ArrayList<Chat>> onCreateLoader(int id, Bundle args) {
            ArrayList<Chat> deleted = Parcels.unwrap(args.getParcelable(CHATS_EXTRA));
            return new DeleteChatsAsyncTaskLoader(getActivity(), deleted);
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<Chat>> loader, ArrayList<Chat> deleted) {
            hideProgressDialog();
            getLoaderManager().destroyLoader(LOADER_ID_DELETE_CHATS);
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<Chat>> loader) {

        }
    };

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private static class LoadChatsAsyncTaskLoader extends ContentResolverAsyncTaskLoader<ArrayList<ChatItem>> {

        private LoadChatsAsyncTaskLoader(Context context) {
            super(context);
        }

        @Nullable
        @Override
        public ArrayList<ChatItem> loadInBackground(@NonNull ContentResolver contentResolver) {
            final ArrayList<ChatItem> chatItems = new ArrayList<>();

            Logger.onOperationStart("LoadChats");
            ArrayList<Chat> chats = DataProviderHelper.getChats(contentResolver);

            if (CollectionUtils.isNotEmpty(chats)) {
                Collections.sort(chats, CHAT_COMPARATOR);
                chatItems.addAll(Stream.of(chats)
                        .filter(value -> CollectionUtils.isNotEmpty(value.getContacts()))
                        .map(ChatItem::new)
                        .collect(Collectors.toList()));
            }
            Logger.onOperationEnd("LoadChats", chatItems.size());

            return chatItems;
        }
    }

    @Override
    public Loader<ArrayList<ChatItem>> onCreateLoader(int id, Bundle args) {
        return new LoadChatsAsyncTaskLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<ChatItem>> loader, ArrayList<ChatItem> data) {
        mChatItems.clear();

        if (CollectionUtils.isNotEmpty(data)) {
            mChatItems.addAll(data);
        }

        Utils.setVisibility(mProgress, View.GONE);

        if (mChatItems.isEmpty()) {
            Utils.setVisibility(mNoDataText, View.VISIBLE);
            Utils.setVisibility(mRecyclerView, View.GONE);
        } else {
            Utils.setVisibility(mNoDataText, View.GONE);
            Utils.setVisibility(mRecyclerView, View.VISIBLE);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }

        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<ChatItem>> loader) {
    }

    @Override
    public void onChatCreated(Chat chat) {
        hideProgressDialog();

        if (chat != null) {
            startChatActivity(chat);
            updateList();

//            mChatItems.add(0, new ChatItem(chat));
//            mRecyclerView.getAdapter().notifyDataSetChanged();

        } else {
            Utils.showAlertText(getActivity(), R.string.unable_create_chat);
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    static class ChatItem {
        private Chat mChat;
        private boolean mDeleting;

        public ChatItem(Chat chat) {
            mChat = chat;
        }

        public Chat getChat() {
            return mChat;
        }

        public ChatMessage getLastMessage() {
            return mChat.getLastMessage();
        }

        public ArrayList<Contact> getContacts() {
            return mChat.getContacts();
        }

        public boolean isDeleting() {
            return mDeleting;
        }

        public void setDeleting(boolean deleting) {
            mDeleting = deleting;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateList();

        LocalBroadcast.registerReceiver(getActivity(), mBroadcastReceiver,
                DataProvider.ACTION_CHATS_UPDATED,
                DataProvider.ACTION_CHATS_DELETED,
                ContactsManager.ACTION_USERS_STATUSES_UPDATED,
                BaseManager.AVATAR_LOADED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(getActivity(), mBroadcastReceiver);
    }

    private void updateList() {
        getLoaderManager().initLoader(LOADER_ID_GET_CHATS, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chats_tab_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new ChatsAdapter());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.chats_tab, menu);

        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (!menuVisible && isAdded()) {
            changeState(false, false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_to_deleting_chats) {
            changeState(true);
            return true;
        } else if (id == R.id.action_create_new_chat) {
            startActivityForResult(SelectContactsActivity.newIntent(getActivity()), REQUEST_CODE_SELECT_ACCOUNTS);
            return true;
        } else if (id == R.id.action_set_all_chats_read) {
            new Thread(BL::MarkAllMessagesAsRead).start();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteChats() {
        ArrayList<Chat> deletedChats = Stream.of(mChatItems)
                .filter(ChatItem::isDeleting)
                .map(ChatItem::getChat)
                .collect(Collectors.toCollection(ArrayList::new));

        if (CollectionUtils.isNotEmpty(deletedChats)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(getResources().getQuantityString(R.plurals.delete_chats_count, deletedChats.size(), deletedChats.size()))
                    .setMessage(R.string.alert_dialog_message_delete_chats_confirm)
                    .setPositiveButton(R.string.yes_delete_chats, (dialog, which) -> {
                        changeState(false, false);

                        mProgressDialog = Utils.showProgress(getActivity(), R.string.progress_message_delete_chats);

                        Bundle b = new Bundle();
                        b.putParcelable(CHATS_EXTRA, Parcels.wrap(deletedChats));

                        getLoaderManager().initLoader(LOADER_ID_DELETE_CHATS, b, mDeleteChatsLoaderCallbacks);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            getActivity().getMenuInflater().inflate(R.menu.delete_chats_menu, menu);
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
                deleteChats();
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            changeState(false, false);
        }
    };

    private void changeState(boolean stateDeleting) {
        changeState(stateDeleting, true);
    }

    private void changeState(boolean stateDeleting, boolean deselectAll) {
        mStateDeleting = stateDeleting;

        View toolbar = getActivity().findViewById(R.id.toolbar);

        if (mStateDeleting) {
            toolbar.startActionMode(mActionModeCallback);
        } else if (mActionMode != null) {
            mActionMode.finish();
        }

        if (deselectAll) {
            selectAll(false);
        } else {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_ACCOUNTS) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<Contact> selectedContacts = SelectContactsActivity.extractResult(data);
                if (CollectionUtils.isNotEmpty(selectedContacts)) {

                    mProgressDialog = Utils.showProgress(getActivity(), R.string.progress_message_creating_chat);
                    CreateChatAsyncTask.execute(getActivity(), selectedContacts, this);
                }
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    class ChatsViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.delete_check)
        CheckBox deleteCheck;
        @BindView(R.id.contact_avatar)
        RoundedImageView contactAvatar;
        @BindView(R.id.date_time_text)
        TextView dateTimeText;
        @BindView(R.id.name_text)
        TextView nameText;
        @BindView(R.id.users_count_text)
        TextView usersCountText;
        @BindView(R.id.message_text)
        TextView messageText;
        @BindView(R.id.new_messages_counter_text)
        TextView newMessagesCounterText;

        ChatsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class ChatsAdapter extends RecyclerView.Adapter<ChatsViewHolder> {

        final int mReadColor;
        final int mUnreadColor;

        private ChatsAdapter() {
            mUnreadColor = ContextCompat.getColor(getActivity(), R.color.read_message_text);
            mReadColor = ContextCompat.getColor(getActivity(), R.color.unread_message_text);
        }

        @Override
        public ChatsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ChatsViewHolder(getActivity().getLayoutInflater().inflate(R.layout.chats_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final ChatsViewHolder holder, final int position) {
            final ChatItem item = mChatItems.get(position);
            final Chat chat = item.getChat();
            final ChatMessage message = item.getLastMessage();

            if (!chat.isP2p()) {
                holder.contactAvatar.setImageResource(R.drawable.no_photo_group);

                holder.nameText.setCompoundDrawables(null, null, null, null);

                if (TextUtils.isEmpty(chat.getTitle())) {
                    holder.nameText.setText(R.string.chat_no_members);
                } else {
                    holder.nameText.setText(chat.getTitle());
                }


                Utils.setVisibility(holder.usersCountText, View.VISIBLE);

                int usersCount = chat.getContacts().size() - 1;
                holder.usersCountText.setText(getResources().getQuantityString(R.plurals.chat_accounts_count, usersCount, usersCount));
            } else {
                Contact c = Utils.p2pChatPartner(chat);
                holder.contactAvatar.setUrl(c.avatarPath);

                Drawable statusNameDrawable = Utils.getDrawable(getActivity(), R.drawable.contacts_list_item_status_ic);
                if (statusNameDrawable != null) {
                    int w = statusNameDrawable.getIntrinsicWidth();
                    int h = statusNameDrawable.getIntrinsicHeight();

                    statusNameDrawable.setBounds(0, 0, w, h);
                    statusNameDrawable.setLevel(StatusesAdapter.getStatusDrawableLevel(c.getStatus()));
                }

                holder.nameText.setCompoundDrawables(statusNameDrawable, null, null, null);
                holder.nameText.setText(Utils.formatAccountFullName(c));
//                holder.nameText.getCompoundDrawables()[0].setLevel(StatusesAdapter.getStatusDrawableLevel(c.baseStatus));

                Utils.setVisibility(holder.usersCountText, View.GONE);
            }


            holder.messageText.setMaxLines(chat.isP2p() ? 2 : 1);
            CharSequence lastMessageText = null;
            if (message != null) {
                holder.messageText.setTextColor(message.isRead() ? mReadColor : mUnreadColor);
                String content = message.getContent();

                if (message.isEncrypted()) {
                    lastMessageText = getString(R.string.chat_message_encrypted);
                } else if (message.isTextMessage()) {
                    lastMessageText = content;
                } else if (message.isQuoteMessage()) {
                    lastMessageText = !TextUtils.isEmpty(content) ? content : Utils.buildChatMessageText(getActivity(), message);
                } else if (message.isSubjectMessage()) {
                    lastMessageText = getString(R.string.chat_has_rename, content);
                } else if (message.isNotificationMessage()) {
                    ChatNotificationData cnd = message.getNotificationData();
                    if (cnd != null) {
                        lastMessageText = Utils.buildChatNotificationMessageText(getActivity(), message.getSender(), cnd);
                    }
                } else if (message.isContactMessage() || message.isDeletedMessage()) {
                    lastMessageText = Utils.buildChatMessageText(getActivity(), message);
                }
            }
            holder.messageText.setText(lastMessageText);

            int newMessagesCount = chat.getNewMessagesCount();
            if (newMessagesCount > 0) {
                Utils.setVisibility(holder.newMessagesCounterText, View.VISIBLE);
                holder.newMessagesCounterText.setText(String.valueOf(newMessagesCount));
            } else {
                Utils.setVisibility(holder.newMessagesCounterText, View.GONE);
            }

            holder.dateTimeText.setText(Utils.formatDateTimeShort(chat.getLastModifiedDate()));

            if (mStateDeleting) {
                Utils.setVisibility(holder.deleteCheck, View.VISIBLE);

                holder.deleteCheck.setOnCheckedChangeListener(null);
                holder.deleteCheck.setChecked(item.isDeleting());
                holder.deleteCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.setDeleting(isChecked);
                    setSelectedChats(mSelectedChats + (isChecked ? 1 : -1));
                });
            } else {
                Utils.setVisibility(holder.deleteCheck, View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                        if (mStateDeleting) {
                            boolean checked = !holder.deleteCheck.isChecked();
                            holder.deleteCheck.setChecked(checked);
                        } else {
                            startChatActivity(chat);
                        }
                    }
            );

            holder.itemView.setOnLongClickListener(v -> {
                        if (!mStateDeleting) {
                            for (ChatItem i : mChatItems) {
                                i.setDeleting(i == item);
                            }

                            changeState(true, false);
                            setSelectedChats(1);
                            return true;
                        }

                        return false;
                    }
            );
        }

        @Override
        public int getItemCount() {
            return mChatItems.size();
        }

    }

    private void startChatActivity(Chat chat) {
        mFragmentActionListener.onOpenChat(chat);
    }

    private void setSelectedChats(int selectedChats) {
        mSelectedChats = selectedChats;

        if (mActionMode != null) {
            mActionMode.setTitle(getResources().getString(R.string.toolbar_selected_chats_count, mSelectedChats));
        }
    }
}
