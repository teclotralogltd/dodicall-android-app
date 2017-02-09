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

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.joda.time.DateTimeComparator;
import org.parceler.Parcels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.MessagesAdapter;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.api.MessageItem;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.fragment.ChatActionsBottomSheetFragment;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.ChatMessagesTaskLoader;
import ru.swisstok.dodicall.task.GetChatAsyncTaskLoader;
import ru.swisstok.dodicall.task.GetMeAsyncTaskLoader;
import ru.swisstok.dodicall.task.SendChatMessageAsyncTask;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Logger;
import ru.swisstok.dodicall.util.NotificationsUtils;
import ru.swisstok.dodicall.util.RemoveFormattingTextWatcher;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;

public class ChatActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<ArrayList<MessageItem>>,
        SendChatMessageAsyncTask.OnSendChatMessageListener,
        ChatActionsBottomSheetFragment.OnContentSourceActionListener {

    public static final String EXTRA_CHAT = "extra_chat";
    public static final String EXTRA_FORWARDED_MESSAGES = "extra_forwarded_messages";
    public static final String EXTRA_SHOW_KEYBOARD = "extra_showKeyboard";

    private static final int REQUEST_CODE_SELECT_ACCOUNTS = 200;
    private static final int REQUEST_CODE_SELECT_ACCOUNTS_FOR_CONFERENCE = 201;
    private static final int MIN_VISIBLE_ELEMENT_NUMBER = 8;
    private static final int LOADER_ID_GET_MESSAGES = 900;
    private static final int LOADER_ID_UPDATE_CHAT = 901;
    private static final int LOADER_ID_ME = 902;
    private static final String ID = "id";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.chat_title_text)
    TextView mChatTitleText;

    @BindView(R.id.chat_sub_title_text)
    TextView mChatSubTitleText;

    @BindView(R.id.messages_recycler)
    RecyclerView mRecyclerView;

    @BindView(R.id.message_edit)
    EditText mMessageEdit;

    @BindView(R.id.message_container)
    ViewGroup mEditMessageContainer;

    @BindView(R.id.attach_button)
    ImageButton mAttachButton;

    @BindView(R.id.voice_button)
    ImageButton mVoiceButton;

    @BindView(R.id.picture_button)
    ImageButton mPictureButton;

    @BindView(R.id.send_button)
    ImageButton mSendButton;

    @BindView(R.id.progress)
    ProgressBar mProgress;

    @BindView(R.id.reply_container)
    ViewGroup mReplyContainer;

    @BindView(R.id.reply_clear)
    ImageButton mReplyClear;

    @BindView(R.id.reply_user)
    TextView mReplyUser;

    @BindView(R.id.reply_time)
    TextView mReplyTime;

    @BindView(R.id.reply_text)
    TextView mReplyText;

    private ChatActionsBottomSheetFragment mChatActionsBottomSheetFragment;

    private MessagesAdapter mMessagesAdapter;
    private ActionMode mActionMode;

    private Chat mChat;
    private String mFirstLoadedMessageId = "";
    private boolean mChatMessagesLoading;
    private ArrayList<MessageItem> mMessages = new ArrayList<>();
    private boolean mLastElementVisible = false;
    private Contact mMe;

    private String mReplyActionText;
    private String mCopyActionText;
    private String mSelectActionText;
    private String mEditActionText;
    private String mDeleteActionText;
    private String mInfoActionText;
    private List<ChatMessage> mMessagesToReply = new ArrayList<>();
    private ChatMessage.MessageAction mMessageAction;

    private final DateTimeComparator mDayComparator = DateTimeComparator.getDateOnlyInstance();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (TextUtils.equals(action, DataProvider.ACTION_CHAT_MESSAGES_UPDATED)) {
                getSupportLoaderManager().initLoader(LOADER_ID_GET_MESSAGES, intent.getExtras(), ChatActivity.this);
            } else if (TextUtils.equals(action, DataProvider.ACTION_CHATS_UPDATED)) {
                String[] ids = DataProvider.extractExtraIds(intent);

                if (ids != null) {
                    for (String id : ids) {
                        if (mChat.getId().equals(id)) {
                            requestChat(id);
                            break;
                        }
                    }
                }
            } else if (TextUtils.equals(action, DataProvider.ACTION_CHATS_RECREATED)) {
                String[] ids = DataProvider.extractExtraIds(intent);

                if (ids != null) {
                    if (mChat.getId().equals(ids[0])) {
                        getSupportLoaderManager().destroyLoader(LOADER_ID_UPDATE_CHAT);
                        requestChat(ids[1]);
                    }
                }
            } else if (TextUtils.equals(action, ContactsManager.ACTION_USERS_STATUSES_UPDATED)) {
                List<String> ids = (List<String>) intent.getSerializableExtra(BaseManager.EXTRA_DATA);

                if (!CollectionUtils.isEmpty(ids)) {
                    new Thread(() -> {
                        for (String id : ids) {
                            ContactStatus contactStatus = BL.getContactStatus(id);

                            Stream.of(mChat.getContacts())
                                    .filter(c -> TextUtils.equals(c.xmppId, id))
                                    .forEach((Contact c) -> c.contactStatus = contactStatus);

                            Stream.of(mMessages)
                                    .filter(mi -> mi.getMessage() != null && mi.getMessage().getSender() != null && TextUtils.equals(mi.getMessage().getSender().xmppId, id))
                                    .forEach((MessageItem mi) -> mi.getMessage().getSender().contactStatus = contactStatus);
                        }

                        mRecyclerView.post(() -> {
                            updateChatInfo();
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        });
                    }).start();
                }
            } else if (BaseManager.AVATAR_LOADED.equals(intent.getAction())) {
                Contact contact = (Contact) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                for (MessageItem item : mMessages) {
                    ChatMessage chatMessage = item.getMessage();
                    if (chatMessage != null) {
                        if (chatMessage.getSender().dodicallId.equals(contact.dodicallId)) {
                            chatMessage.setSender(contact);
                        }
                        if (chatMessage.isContactMessage() &&
                                chatMessage.getSharedContact().dodicallId.equals(contact.dodicallId)) {
                            chatMessage.setSharedContact(contact);
                        }
                    }
                }
                mMessagesAdapter.notifyDataSetChanged();
            }
        }
    };

    private LoaderManager.LoaderCallbacks<Chat> mChatLoaderCallback = new LoaderManager.LoaderCallbacks<Chat>() {
        @Override
        public Loader<Chat> onCreateLoader(int id, Bundle args) {
            String chatId = args.getString(ID);
            return new GetChatAsyncTaskLoader(ChatActivity.this, chatId);
        }

        @Override
        public void onLoadFinished(Loader<Chat> loader, Chat data) {
            if (data != null) {
                String chatId = mChat.getId();

                if (chatId.equals(data.getId()) && !mChat.isActive() && data.isActive()) {
                    mMessageEdit.setText(null);
                }

                mChat = data;
                updateChatInfo();

                if (!chatId.equals(data.getId())) {
                    mFirstLoadedMessageId = "";
                    mMessages.clear();
                    setupRecyclerView();
                    Utils.setVisibility(mProgress, View.VISIBLE);
                    getSupportLoaderManager().destroyLoader(LOADER_ID_GET_MESSAGES);
                    getSupportLoaderManager().initLoader(LOADER_ID_GET_MESSAGES, null, ChatActivity.this);
                }
            }

            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Chat> loader) {

        }
    };

    private LoaderManager.LoaderCallbacks<Contact> mMeLoaderCallback = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            return new GetMeAsyncTaskLoader(ChatActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            mMe = data;
            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mChat = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CHAT));

        mReplyActionText = getString(R.string.chat_message_action_answer);
        mCopyActionText = getString(android.R.string.copy);
        mSelectActionText = getString(R.string.chat_message_action_select);
        mEditActionText = getString(R.string.chat_message_action_edit);
        mDeleteActionText = getString(R.string.chat_message_action_remove);
        mInfoActionText = getString(R.string.chat_message_action_info);

        setupActionBar();
        setupRecyclerView();

        LocalBroadcast.registerReceiver(this, mBroadcastReceiver,
                DataProvider.ACTION_CHAT_MESSAGES_UPDATED,
                DataProvider.ACTION_CHATS_UPDATED,
                DataProvider.ACTION_CHATS_RECREATED,
                ContactsManager.ACTION_USERS_STATUSES_UPDATED,
                BaseManager.AVATAR_LOADED);


        //noinspection WrongThread
        mMessageEdit.setTextSize(BusinessLogic.GetInstance().GetUserSettings().getGuiFontSize());

        mMessageEdit.addTextChangedListener(new RemoveFormattingTextWatcher());

        boolean showKeyboard = getIntent().getBooleanExtra(EXTRA_SHOW_KEYBOARD, false);
        if (showKeyboard) {
            mMessageEdit.postDelayed(() -> showKeyboard(mMessageEdit), 200);
        }

        getSupportLoaderManager().initLoader(LOADER_ID_ME, null, mMeLoaderCallback);
        getSupportLoaderManager().initLoader(LOADER_ID_GET_MESSAGES, null, this);

        mChatActionsBottomSheetFragment = new ChatActionsBottomSheetFragment();
        mChatActionsBottomSheetFragment.setOnContentSourceActionListener(this);

        if (getIntent().hasExtra(EXTRA_FORWARDED_MESSAGES)) {
            replyToMessage(Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_FORWARDED_MESSAGES)), ChatMessage.MessageAction.Forward);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mChat != null && mChat.isActive()) {
            String incompleteMessage = mMessageEdit.getText().toString();
            updateIncompleteMessage(incompleteMessage);
        }

        hideKeyboard(mMessageEdit);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcast.unregisterReceiver(this, mBroadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SELECT_ACCOUNTS) {
                ArrayList<Contact> selectedContacts = SelectContactsActivity.extractResult(data);
                if (CollectionUtils.isNotEmpty(selectedContacts)) {
                    Stream.of(selectedContacts).forEach(this::sendContact);
                }

                return;
            } else if (requestCode == REQUEST_CODE_SELECT_ACCOUNTS_FOR_CONFERENCE) {
                ArrayList<Contact> selectedContacts = SelectContactsForConferenceActivity.extractResult(data);
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Nullable
    @Override
    protected NotificationsUtils.NotificationType getNotificationType() {
        return NotificationsUtils.NotificationType.Chat;
    }

    @Override
    protected String getInterestedIdForNotifications() {
        return mChat.getId();
    }

    @Override
    protected boolean isWithNotificationUpdate() {
        return true;
    }

    private void requestChat(String id) {
        Bundle b = new Bundle();
        b.putString(ID, id);

        getSupportLoaderManager().initLoader(LOADER_ID_UPDATE_CHAT, b, mChatLoaderCallback);
    }

    private void setupRecyclerView() {
        mMessagesAdapter = new MessagesAdapter(this, mChat.getId(), mMessages, mChat.isP2p(), new MessagesAdapter.SelectionListener() {
            @Override
            public void onMessageSelected(MessageItem messageItem, boolean canBeDeleted, boolean canBeEdited, boolean canBeCopied) {
                if (!(messageItem.getMessage().isTextMessage() || messageItem.getMessage().isContactMessage())) {
                    return;
                }
                List<String> actions = new ArrayList<>();
                actions.add(mReplyActionText);
                if (canBeCopied) {
                    actions.add(mCopyActionText);
                }
                actions.add(mSelectActionText);
                if (messageItem.getSender().iAm) {
                    if (canBeEdited) {
                        actions.add(mEditActionText);
                    }
                    if (canBeDeleted) {
                        actions.add(mDeleteActionText);
                    }
                }
                actions.add(mInfoActionText);
                new AlertDialog.Builder(ChatActivity.this)
                        .setTitle(R.string.chat_message_actions_title)
                        .setItems(actions.toArray(new String[actions.size()]), (dialog, which) -> {
                            String action = actions.get(which);
                            if (action.equals(mReplyActionText)) {
                                replyToMessage(Collections.singletonList(messageItem.getMessage()), ChatMessage.MessageAction.Reply);
                            } else if (action.equals(mCopyActionText)) {
                                Utils.copyTextToClipboard(ChatActivity.this, messageItem.getMessage().getContent(), R.string.chat_message_copied_to_clipboard);
                            } else if (action.equals(mSelectActionText)) {
                                selectMessages(messageItem);
                            } else if (action.equals(mEditActionText)) {
                                startActivity(EditMessageActivity.newIntent(ChatActivity.this, messageItem.getMessage()));
                            } else if (action.equals(mDeleteActionText)) {
                                deleteMessages(Collections.singletonList(messageItem.getMessage()));
                            } else if (action.equals(mInfoActionText)) {
                                showMessageInfo(messageItem.getMessage());
                            }
                        }).show();
            }

            @Override
            public void onMessageSelected(int selectedCount) {
                setupSelectedMessagesCount(selectedCount);
            }

            @Override
            public void onContactSelected(Contact contact) {
                startActivity(ProfileActivity.newIntent(ChatActivity.this, contact));
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mMessagesAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!(mMessages.size() < BL.CHAT_MESSAGES_PAGE_SIZE || mChatMessagesLoading)) {
                    int pos = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    if (pos < MIN_VISIBLE_ELEMENT_NUMBER && mMessages.size() < mChat.getTotalMessagesCount()) {
                        for (MessageItem message : mMessages) {
                            ChatMessage chatMessage = message.getMessage();
                            if (chatMessage != null) {
                                mChatMessagesLoading = true;
                                mFirstLoadedMessageId = chatMessage.getId();
                                getSupportLoaderManager().initLoader(LOADER_ID_GET_MESSAGES, null, ChatActivity.this);
                                break;
                            }
                        }
                    }
                }
            }
        });

        mRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                mRecyclerView.postDelayed(() -> {
                    mLastElementVisible = true;
                    scrollToEnd();
                }, 100);
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);
        setTitle(null);

        updateChatInfo();
    }

    private void updateChatInfo() {
        ArrayList<Contact> contacts = mChat.getContacts();

        if (!mChat.isP2p()) {
            mChatTitleText.setCompoundDrawables(null, null, null, null);

            if (TextUtils.isEmpty(mChat.getTitle())) {
                mChatTitleText.setText(R.string.chat_no_members);
            } else {
                mChatTitleText.setText(mChat.getTitle());
            }

            if (contacts == null || contacts.isEmpty()) {
                Toast.makeText(this, "ERROR: Contacts list null or empty!!!!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            int usersCount = contacts.size() - 1;
            mChatSubTitleText.setText(getResources().getQuantityString(R.plurals.chat_accounts_count, usersCount, usersCount));
        } else {
            Contact c = Utils.p2pChatPartner(mChat);

            Drawable statusNameDrawable = getResources().getDrawable(R.drawable.contacts_list_item_status_ic);

            if (statusNameDrawable != null) {
                statusNameDrawable.setLevel(StatusesAdapter.getStatusDrawableLevel(c.getStatus()));

                int h = statusNameDrawable.getIntrinsicHeight();
                int w = statusNameDrawable.getIntrinsicWidth();
                statusNameDrawable.setBounds(0, 0, w, h);

                mChatTitleText.setCompoundDrawables(statusNameDrawable, null, null, null);
            }

            mChatTitleText.setText(Utils.formatAccountFullName(c));

            //TODO: Set last user login datetime
            mChatSubTitleText.setText(null);
        }

        mChatTitleText.setOnClickListener(v -> startActivity(ManageUsersOfChatActivity.newIntent(this, mChat)));

        if (mChat.isActive()) {
            Utils.viewGroupEnabled(mEditMessageContainer, true);

            String incompleteMessage = mChat.getIncompleteMessage();
            if (!TextUtils.isEmpty(incompleteMessage)) {
                mMessageEdit.setText(incompleteMessage);
                mMessageEdit.setSelection(incompleteMessage.length());
            }
        } else {
            Utils.viewGroupEnabled(mEditMessageContainer, false);
            mMessageEdit.setText(R.string.chat_is_not_active);
        }
    }

    @Override
    public void onChatMessageSent(String id) {
        for (int i = mMessages.size() - 1; i >= 0; i--) {
            MessageItem mi = mMessages.get(i);
            if (mi.isUnreadSeparator()) {
                mMessages.remove(i);
                mRecyclerView.getAdapter().notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public void onContact() {
        startActivityForResult(SelectContactsActivity.newIntent(this), REQUEST_CODE_SELECT_ACCOUNTS);
    }

    @Override
    public void onGallery() {
        Utils.showComingSoon(this);
    }

    @Override
    public void onPhoto() {
        Utils.showComingSoon(this);
    }

    @OnTextChanged(value = R.id.message_edit, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void onMessageTextChanged(CharSequence text) {
        allowSendText(!TextUtils.isEmpty(text));
    }

    private void allowSendText(boolean allow) {
        if (allow) {
            Utils.setVisibilityGone(mVoiceButton);
            Utils.setVisibilityGone(mPictureButton);
            Utils.setVisibilityVisible(mSendButton);
        } else {
            Utils.setVisibilityVisible(mVoiceButton);
            Utils.setVisibilityVisible(mPictureButton);
            Utils.setVisibilityGone(mSendButton);
        }
    }

    @OnClick(R.id.attach_button)
    void selectContentToAttach() {
        mChatActionsBottomSheetFragment.show(getSupportFragmentManager(), mChatActionsBottomSheetFragment.getTag());
    }

    @OnClick(R.id.reply_clear)
    void clearReply() {
        mMessagesToReply.clear();
        Utils.setVisibilityGone(mReplyContainer);
        boolean isWithText = !TextUtils.isEmpty(mMessageEdit.getText());
        allowSendText(isWithText);
        if (isWithText) {
            showKeyboard(mMessageEdit);
        }
    }

//    @OnClick(R.id.picture_button)
//    void sendTestMessages() {
//        new Thread(() -> {
//            try {
//                File f = new File("/sdcard/domik");
//                if (f.exists() && f.canRead()) {
//                    char[] buffer = new char[(int) f.length()];
//                    FileReader fr = new FileReader(f);
//                    int len = fr.read(buffer);
//
//                    for (int i = 0, j = 0; i < len - 1; i++) {
//                        if (buffer[i] == '\n' && buffer[i + 1] == '\n') {
//                            int textLen = i - j;
//                            char[] text = new char[textLen];
//
//                            System.arraycopy(buffer, j, text, 0, textLen);
//                            String str = new String(text);
//
////                            Log.d("aaa", str);
//
//                            String id = BusinessLogic.GetInstance().PregenerateMessageId();
//                            BusinessLogic.GetInstance().SendTextMessage(id, mChat.getId(), str);
//
//                            j = i + 1;
//
//                            Thread.sleep(500);
//                        }
//
//                        if (i == len - 2 && j < len - 1) {
//                            int textLen = len - j;
//                            char[] text = new char[textLen];
//
//                            System.arraycopy(buffer, j, text, 0, textLen);
//                            String str = new String(text);
//
////                            Log.d("aaa", str);
//
//                            String id = BusinessLogic.GetInstance().PregenerateMessageId();
//                            BusinessLogic.GetInstance().SendTextMessage(id, mChat.getId(), str);
//                        }
//                    }
//
//                    fr.close();
//                }
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }

    @OnClick(R.id.send_button)
    void sendMessage() {
        String messageContent = mMessageEdit.getText().toString();

        String id = BusinessLogic.GetInstance().PregenerateMessageId();
        ChatMessage cm = new ChatMessage(
                id,
                mChat.getId(),
                false, System.currentTimeMillis(),
                false,
                false,
                messageContent,
                ChatMessage.TYPE_TEXT_MESSAGE,
                mMe,
                null,
                1,
                false,
                null);
        if (!mMessagesToReply.isEmpty()) {
            cm.setQuotedMessages(new ArrayList<>(mMessagesToReply));
        }

        sendMessage(cm);

        clearReply();

        cm.setContent(messageContent);
    }

    private void sendContact(Contact contact) {
        String id = BusinessLogic.GetInstance().PregenerateMessageId();
        ChatMessage cm = new ChatMessage(
                id,
                mChat.getId(),
                false, System.currentTimeMillis(),
                false,
                false,
                "",
                ChatMessage.TYPE_CONTACT,
                mMe,
                null,
                1,
                false,
                contact);

        sendMessage(cm);
    }

    private void sendMessage(ChatMessage chatMessage) {
        if (CollectionUtils.isEmpty(mMessages)) {
            mMessages.add(new MessageItem(System.currentTimeMillis()));
        } else {
            boolean needInsertDateSeparator = false;
            boolean existDateSeparator = false;
            int i = mMessages.size() - 1;

            for (; i >= 0; --i) {
                MessageItem mi = mMessages.get(i);

                if (mi.getDate() != null && mDayComparator.compare(System.currentTimeMillis(), mi.getDate()) == 0) {
                    existDateSeparator = true;
                    break;
                }

                if (mi.getMessage() != null && mDayComparator.compare(System.currentTimeMillis(), mi.getMessage().getSendTime()) != 0) {
                    needInsertDateSeparator = true;
                    break;
                }
            }

            if (!existDateSeparator) {
                if (i == 0) {
                    mMessages.add(0, new MessageItem(System.currentTimeMillis()));
                }

                if (needInsertDateSeparator) {
                    mMessages.add(i + 1, new MessageItem(System.currentTimeMillis()));
                }
            }
        }

        mMessages.add(new MessageItem(chatMessage, true));
        mRecyclerView.getAdapter().notifyItemInserted(mMessages.size() - 1);

        mLastElementVisible = true;
        scrollToEnd();

        SendChatMessageAsyncTask.execute(chatMessage, mMessageAction, this);
        mMessageEdit.setText(null);
        mMessageAction = null;
        updateIncompleteMessage(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            hideKeyboard(mMessageEdit);
            finish();
            return true;
        } else if (id == R.id.action_voice_call) {
            if (mChat.isP2p()) {
                OutgoingCallActivity.start(this, Utils.p2pChatPartner(mChat));
            } else {
                Utils.showComingSoon(this);
//                startActivityForResult(SelectContactsForConferenceActivity.newIntent(this, mChat.getContacts()), REQUEST_CODE_SELECT_ACCOUNTS_FOR_CONFERENCE); //todo uncomment when logic will be ready
            }
            return true;
        } else if (id == R.id.action_video_call) {
            Utils.showComingSoon(this);
            return true;
        } else if (id == R.id.action_chat_settings) {
            startActivity(ChatSettingsActivity.newIntent(this, mChat));
            return true;
        } else if (id == R.id.action_manage_users) {
            startActivity(ManageUsersOfChatActivity.newIntent(this, mChat));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void updateIncompleteMessage(String incompleteMessage) {
        mChat.setIncompleteMessage(incompleteMessage);
        ContentValues contentValues = new ContentValues();
        contentValues.put(Chat.COLUMN_INCOMPLETE_MESSAGE, mChat.getIncompleteMessage());
        getContentResolver().update(DataProvider.makeChatIncompleteMessageUpdateUri(mChat.getId()), contentValues, null, null);
    }

    private void setupState(boolean isSelecting) {
        if (isSelecting) {
            mToolbar.startActionMode(mActionModeCallback);
        } else if (mActionMode != null) {
            mActionMode.finish();
        }
        mMessagesAdapter.setSelecting(isSelecting);
        mMessageEdit.setEnabled(!isSelecting);
        hideKeyboard(mMessageEdit);
    }

    private void replyToMessage(List<ChatMessage> messages, ChatMessage.MessageAction messageAction) {
        mMessagesToReply.clear();
        mMessagesToReply.addAll(messages);
        mMessageAction = messageAction;
        if (mMessagesToReply.size() == 1) {
            ChatMessage message = mMessagesToReply.get(0);
            mReplyUser.setText(Utils.formatAccountFullName(message.getSender()));
            mReplyTime.setText(Utils.formatTime(message.getSendTime()));
            mReplyTime.setVisibility(View.VISIBLE);
            mReplyText.setText(message.getContent());
        } else {
            mReplyUser.setText(messageAction == ChatMessage.MessageAction.Forward ? R.string.chat_messages_forward : R.string.chat_messages_quoted);
            mReplyTime.setVisibility(View.GONE);
            mReplyText.setText(getString(R.string.chat_messages_quote_forward_count, messages.size()));
        }
        Utils.setVisibilityVisible(mReplyContainer);
        allowSendText(messageAction != ChatMessage.MessageAction.Reply);
    }

    private void selectMessages(MessageItem messageItem) {
        setupState(true);
        messageItem.setSelected(true);
        mMessagesAdapter.setSelecting(true, 1);
    }

    private ArrayList<ChatMessage> selectedMessages() {
        return Stream.of(mMessages)
                .filter(MessageItem::isSelected)
                .map(MessageItem::getMessage)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void deleteMessages(List<ChatMessage> removedMessages) {
        if (CollectionUtils.isNotEmpty(removedMessages)) {
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getQuantityString(R.plurals.delete_messages_count, removedMessages.size(), removedMessages.size()))
                    .setMessage(R.string.alert_dialog_message_delete_messages_confirm)
                    .setPositiveButton(R.string.yes_delete_chats, (dialog, which) -> {
                        setupState(false);

                        BL.deleteMessages(removedMessages);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    private void showMessageInfo(ChatMessage chatMessage) {
        Utils.showComingSoon(this);
    }

    private void selectAll(boolean select) {
        mMessagesAdapter.selectAll(select);
    }

    private void setupSelectedMessagesCount(int selectedMessages) {
        if (mActionMode != null) {
            mActionMode.setTitle(getResources().getString(R.string.toolbar_selected_chats_count, selectedMessages));
        }
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            getMenuInflater().inflate(R.menu.chat_messages_action_menu, menu);
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
            } else if (id == R.id.action_copy) {
                copyMessages();
                setupState(false);
                return true;
            } else if (id == R.id.action_quote) {
                replyToMessage(selectedMessages(), ChatMessage.MessageAction.Quote);
                setupState(false);
                return true;
            } else if (id == R.id.action_forward) {
                startActivity(
                        new Intent(getApplicationContext(), ForwardMessagesActivity.class).
                                putExtra(ForwardMessagesActivity.EXTRA_FORWARDED_MESSAGES_LIST, Parcels.wrap(selectedMessages())).
                                putExtra(ForwardMessagesActivity.EXTRA_INITIAL_CHAT_ID, mChat.getId())
                );
                setupState(false);
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            setupState(false);
        }
    };

    private void copyMessages() {
        final SimpleDateFormat timeDateSdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        StringBuilder sb = new StringBuilder();

        for (ChatMessage message : selectedMessages()) {
            String content = "";

            if (message.isTextMessage()) {
                content = message.getContent();
            } else if (message.isContactMessage()) {
                content = Utils.formatAccountFullName(message.getSharedContact());
            }

            sb.append('[')
                    .append(timeDateSdf.format(new Date(message.getSendTime())))
                    .append("] ").append(Utils.formatAccountFullName(message.getSender())).append(": ")
                    .append(content)
                    .append('\n');
        }

        Utils.copyTextToClipboard(this, sb.toString(), R.string.chat_messages_copied_to_clipboard);
    }

    @Override
    public Loader<ArrayList<MessageItem>> onCreateLoader(int id, Bundle args) {
        String[] ids = args == null ? null : args.getStringArray(DataProvider.EXTRA_IDS);
        mLastElementVisible = mMessages.isEmpty() || isLastItemDisplaying();
        Logger.onOperationStart("LoadChatMessages");
        return new ChatMessagesTaskLoader(this, mChat.getId(), args == null ? mFirstLoadedMessageId : null, ids, mMessages);
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<MessageItem>> loader, ArrayList<MessageItem> data) {
        Utils.setVisibility(mProgress, View.GONE);

        Logger.onOperationEnd("LoadChatMessages");

        if (mChatMessagesLoading) {
            if (!CollectionUtils.isEmpty(data)) {
//                mRecyclerView.getAdapter().notifyItemRangeInserted(0, data.size());
                mRecyclerView.getAdapter().notifyDataSetChanged();
                scrollToEnd();
            }
        } else {
            mRecyclerView.getAdapter().notifyDataSetChanged();
            scrollToEnd();
        }

        if (CollectionUtils.isNotEmpty(data) && CollectionUtils.isNotEmpty(mMessages)) {
            if (!data.get(data.size() - 1).getMessage().isRead()) {
                if (data.get(0).getMessage().isRead() || mMessages.size() >= mChat.getTotalMessagesCount()) {
                    new Thread(() -> getContentResolver().update(DataProvider.makeChatMessagesSetReadUri(mChat.getId(), mMessages.get(mMessages.size() - 1).getMessage().getId()), null, null, null)).
                            start();
                }
            }
        }
        mChatMessagesLoading = false;

        getSupportLoaderManager().destroyLoader(loader.getId());

        Log.d("aaa", "onLoadFinished() - end");
    }

    private boolean isLastItemDisplaying() {
        if (mRecyclerView.getAdapter().getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == mRecyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return false;
    }

    private void scrollToEnd() {
        if (mLastElementVisible) {
            mRecyclerView.getLayoutManager().scrollToPosition(mRecyclerView.getAdapter().getItemCount() - 1);
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<MessageItem>> loader) {

    }

    public static Intent newIntent(Context context, @NonNull Chat chat, List<ChatMessage> forwardedMessages) {
        return newIntent(context, chat, forwardedMessages, false);
    }

    public static Intent newIntent(Context context, @NonNull Chat chat) {
        return newIntent(context, chat, false);
    }

    public static Intent newIntent(Context context, @NonNull Chat chat, boolean showKeyboard) {
        return newIntent(context, chat, null, showKeyboard);
    }

    public static Intent newIntent(Context context, @NonNull Chat chat, List<ChatMessage> forwardedMessages, boolean showKeyboard) {
        Intent intent = new Intent(context, ChatActivity.class)
                .putExtra(EXTRA_CHAT, Parcels.wrap(chat))
                .putExtra(EXTRA_SHOW_KEYBOARD, showKeyboard);
        if (!CollectionUtils.isEmpty(forwardedMessages)) {
            intent.putExtra(EXTRA_FORWARDED_MESSAGES, Parcels.wrap(forwardedMessages));
        }
        return intent;
    }
}
