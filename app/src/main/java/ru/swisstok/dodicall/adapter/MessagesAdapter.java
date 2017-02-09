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

package ru.swisstok.dodicall.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.MessageItem;
import ru.swisstok.dodicall.util.DateTimeUtils;
import ru.swisstok.dodicall.util.LongClickableMovementMethod;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.LongClickableContainerView;
import ru.swisstok.dodicall.view.RoundedImageView;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.StringSet;

public class MessagesAdapter extends BaseAdapter<MessageItem, RecyclerView.ViewHolder> {

    private static class OnContactClickListener implements View.OnClickListener {
        private final Contact mContact;
        private final SelectionListener mSelectionListener;

        private OnContactClickListener(Contact contact, SelectionListener selectionListener) {
            mContact = contact;
            mSelectionListener = selectionListener;
        }

        @Override
        public void onClick(View v) {
            mSelectionListener.onContactSelected(mContact);
        }
    }

    private static final long TIME_FROM_PREVIOUS_AVATAR = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_QUOTE_LEVEL = 5;

    private final static int TYPE_MESSAGE = 0;
    private static final int TYPE_MESSAGE_MY = 1;
    private static final int TYPE_MESSAGE_NOTIFICATION = 2;
    private static final int TYPE_DATE_SEPARATOR = 3;
    private static final int TYPE_UNREAD_SEPARATOR = 4;
    private static final int TYPE_MESSAGE_SUBJECT = 5;
    private final static int TYPE_REMOVED_MESSAGE_MY = 6;
    private final static int TYPE_REMOVED_MESSAGE = 7;
    private final static int TYPE_MESSAGE_CONTACT_MY = 8;
    private final static int TYPE_MESSAGE_CONTACT = 9;

    private String mChatId;
    private boolean mIsP2PChat;

    private boolean mSelectingState;
    private int mSelectedMessagesCounter;

    private SelectionListener mSelectionListener;

    private int mPadding;
    private Drawable mStatusNameDrawable;
    private int mFontSize = 14;

    public MessagesAdapter(Context context, String chatId, ArrayList<MessageItem> messages, boolean isP2PChat, SelectionListener selectionListener) {
        super(context, messages);
        mChatId = chatId;
        mIsP2PChat = isP2PChat;
        mSelectionListener = selectionListener;

        mPadding = context.getResources().getDimensionPixelSize(R.dimen.chat_message_padding);
        mStatusNameDrawable = ContextCompat.getDrawable(getContext(), R.drawable.contacts_list_item_status_ic);
        mFontSize = BusinessLogic.GetInstance().GetUserSettings().getGuiFontSize();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;

        switch (viewType) {
            case TYPE_MESSAGE:
            case TYPE_MESSAGE_CONTACT: {
                holder = new MessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_message_item, parent, false));
                break;
            }

            case TYPE_MESSAGE_MY:
            case TYPE_MESSAGE_CONTACT_MY: {
                holder = new MyMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_my_message_item, parent, false));
                break;
            }

            case TYPE_MESSAGE_NOTIFICATION: {
                holder = new NotificationMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_notification_message_item, parent, false));
                break;
            }

            case TYPE_MESSAGE_SUBJECT: {
                holder = new NotificationMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_notification_message_item, parent, false));
                break;
            }

            case TYPE_DATE_SEPARATOR: {
                holder = new DateSeparatorViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_date_separator_item, parent, false));
                break;
            }

            case TYPE_UNREAD_SEPARATOR: {
                holder = new UnreadSeparatorViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_unread_separator_item, parent, false));
                break;
            }
            case TYPE_REMOVED_MESSAGE_MY: {
                holder = new MyRemovedMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_my_removed_message_item, parent, false));
                break;
            }
            case TYPE_REMOVED_MESSAGE: {
                holder = new RemovedMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.chat_removed_message_item, parent, false));
                break;
            }
        }

        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        final MessageItem item = getItem(position);

        if (item.isUnreadSeparator()) {
            return TYPE_UNREAD_SEPARATOR;
        }

        if (item.getDate() != null) {
            return TYPE_DATE_SEPARATOR;
        }

        final ChatMessage chatMessage = item.getMessage();

        if (chatMessage.isTextMessage()) {
            return item.getSender().iAm ? TYPE_MESSAGE_MY : TYPE_MESSAGE;
        } else if (chatMessage.isDeletedMessage()) {
            return item.getSender().iAm ? TYPE_REMOVED_MESSAGE_MY : TYPE_REMOVED_MESSAGE;
        } else if (chatMessage.isSubjectMessage()) {
            return TYPE_MESSAGE_SUBJECT;
        } else if (chatMessage.isContactMessage()) {
            return item.getSender().iAm ? TYPE_MESSAGE_CONTACT_MY : TYPE_MESSAGE_CONTACT;
        } else {
            return TYPE_MESSAGE_NOTIFICATION;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final MessageItem item = getItem(position);
        final int itemViewType = holder.getItemViewType();

        if (itemViewType == TYPE_UNREAD_SEPARATOR) {
            return;
        }

        if (itemViewType == TYPE_DATE_SEPARATOR) {
            bindDateSeparator(item, (DateSeparatorViewHolder) holder);
            return;
        }

        final ChatMessage message = item.getMessage();
        final Contact contact = item.getSender();

        boolean previousIsSame = false;
        if (position > 0) {
            ChatMessage previousChatMessage = getItem(position - 1).getMessage();

            previousIsSame = previousChatMessage != null &&
                    (previousChatMessage.isTextMessage() || previousChatMessage.isDeletedMessage() || previousChatMessage.isContactMessage()) &&
                    item.getMessage() != null &&
                    previousChatMessage.getSender() != null &&
                    item.getMessage().getSender() != null &&
                    TextUtils.equals(previousChatMessage.getSender().dodicallId, item.getMessage().getSender().dodicallId) &&
                    (item.getMessage().getSendTime() - previousChatMessage.getSendTime()) < TIME_FROM_PREVIOUS_AVATAR;
        }

        if (itemViewType == TYPE_MESSAGE_MY || itemViewType == TYPE_MESSAGE_CONTACT_MY) {
            bindMyMessage(item, message, (MyMessageViewHolder) holder, previousIsSame);
        } else if (itemViewType == TYPE_MESSAGE || itemViewType == TYPE_REMOVED_MESSAGE || itemViewType == TYPE_MESSAGE_CONTACT) {
            bindMessage(item, contact, (BaseForeignMessageViewHolder) holder, previousIsSame);
        } else if (itemViewType == TYPE_MESSAGE_NOTIFICATION) {
            bindNotificationMessage(item, (NotificationMessageViewHolder) holder);
        } else if (itemViewType == TYPE_MESSAGE_SUBJECT) {
            bindSubjectMessage(item, (NotificationMessageViewHolder) holder);
        } else if (itemViewType == TYPE_REMOVED_MESSAGE_MY) {
            bindRemovedMyMessage(message, (BaseMessageViewHolder) holder, previousIsSame);
        }
    }

    private void bindDateSeparator(MessageItem item, DateSeparatorViewHolder holder) {
        long date = item.getDate();
        String dateTime;

        if (DateTimeUtils.isToday(date)) {
            dateTime = getContext().getString(R.string.chat_date_separator_today);
        } else if (DateTimeUtils.isYesterday(date)) {
            dateTime = getContext().getString(R.string.chat_date_separator_yesterday);
        } else {
            dateTime = DateTimeFormat.forPattern("dd/MM/yyyy").print(date);
        }

        holder.dateText.setText(dateTime);
    }

    private void bindNotificationMessage(MessageItem item, NotificationMessageViewHolder holder) {
        ChatNotificationData cnd = item.getMessage().getNotificationData();
        if (cnd == null) {
            holder.messageText.setText(null);
            return;
        }

        holder.messageText.setText(Utils.buildChatNotificationMessageText(getContext(), item.getSender(), cnd));
    }

    private void bindSelectableView(RecyclerView.ViewHolder holder, View itemView, CheckBox checkBox, MessageItem item) {
        checkBox.setVisibility(mSelectingState ? View.VISIBLE : View.GONE);
        checkBox.setChecked(item.isSelected());

        ((LongClickableContainerView) itemView).setDeleting(mSelectingState);
        itemView.setSelected(mSelectingState && item.isSelected());

        if (mSelectingState) {
            itemView.setOnClickListener(v -> {
                if (mSelectingState) {
                    MessageItem messageItem = getItem(holder.getAdapterPosition());
                    messageItem.setSelected(!messageItem.isSelected());
                    notifyItemChanged(holder.getAdapterPosition());
                    changeCount(mSelectedMessagesCounter + (messageItem.isSelected() ? 1 : -1));
                }
            });
        } else if (!item.getMessage().isContactMessage()) {
            holder.itemView.setOnClickListener(null);
        }
    }


    private void bindSubjectMessage(MessageItem item, NotificationMessageViewHolder holder) {
        String sb = getContext().getString(R.string.user) +
                " <b>" +
                Utils.formatAccountFullName(item.getSender()) +
                "</b> " +
                getContext().getString(R.string.notification_message_subject) +
                " \"" +
                item.getMessage().getContent() +
                '\"';

        holder.messageText.setText(Html.fromHtml(sb));
    }

    private void bindMessage(MessageItem item, Contact contact, BaseForeignMessageViewHolder holder, boolean previousIsSame) {
        ChatMessage message = item.getMessage();

        if (holder instanceof MessageViewHolder) {
            MessageViewHolder mvh = ((MessageViewHolder) holder);

            bindCommon(message, holder, mvh.messageTimeText, ((MessageViewHolder) holder).messageEditView);

            List<ChatMessage> messages = message.getQuotedMessages();
            if (messages != null) {
                mvh.replyContainer.removeAllViews();
                quoteMessages(mvh.replyContainer, messages, 0);
            } else {
                Utils.setVisibilityGone(mvh.replyContainer);
            }

            bindSelectableView(holder, mvh.itemView, mvh.deleteCheck, item);
        }

        if (!mIsP2PChat) {
            if (mStatusNameDrawable != null) {
                int h = mStatusNameDrawable.getIntrinsicHeight();
                int w = mStatusNameDrawable.getIntrinsicWidth();
                mStatusNameDrawable.setBounds(0, 0, w, h);
                mStatusNameDrawable.setLevel(StatusesAdapter.getStatusDrawableLevel(contact.getStatus()));

                holder.messageAccountText.setCompoundDrawables(mStatusNameDrawable, null, null, null);
            }

            holder.messageAccountText.setText(Utils.formatAccountFullName(contact));

            holder.messageAccountText.setVisibility(previousIsSame
                    ? View.GONE
                    : View.VISIBLE);
        } else {
            holder.messageAccountText.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!mSelectingState) {
                MessageItem messageItem = getItem(holder.getAdapterPosition());
                mSelectionListener.onMessageSelected(messageItem, false, false, messageItem.getMessage().isTextMessage());
                return true;
            }
            return false;
        });


        if (previousIsSame) {
            holder.contactAvatar.setVisibility(View.INVISIBLE);
        } else {
            holder.contactAvatar.setVisibility(View.VISIBLE);
            bindBase(message.getSender(), holder.contactAvatar);
        }

        holder.contactIsDodicall.setVisibility(previousIsSame
                ? View.INVISIBLE
                : View.VISIBLE);

        holder.messageContainer.setBackgroundResource(previousIsSame
                ? R.drawable.left_bubble
                : R.drawable.left_bubble_appendix);

        if (message.getQuotedMessages() != null && holder instanceof MessageViewHolder) {
            boolean withoutContent = TextUtils.isEmpty(message.getContent());
            ((MessageViewHolder) holder).replyContainer.setBackgroundResource(previousIsSame
                    ? withoutContent ? R.drawable.bubble_citation : R.drawable.bubble_citation_w_answer
                    : withoutContent ? R.drawable.left_bubble_appendix_only_citation : R.drawable.left_bubble_appendix_citation);
        }

        holder.itemView.setPadding(
                mPadding,
                previousIsSame ? 0 : mPadding,
                mPadding,
                mPadding);
    }

    @SuppressWarnings("UnusedParameters")
    private void bindMyMessage(MessageItem item, ChatMessage message, MyMessageViewHolder holder, boolean previousIsSame) {
        bindCommon(message, holder, holder.messageTimeText, holder.messageEditView);

        List<ChatMessage> messages = message.getQuotedMessages();
        if (messages != null) {
            holder.replyContainer.removeAllViews();
            quoteMessages(holder.replyContainer, messages, 0);
        } else {
            Utils.setVisibilityGone(holder.replyContainer);
        }

        if (item.isShowStatus()) {
            Utils.setVisibilityVisible(holder.statusSentImage);
            Utils.setVisibilityVisible(holder.statusDeliveredImage);
            Utils.setVisibilityVisible(holder.status3Image);
            Utils.setVisibilityVisible(holder.status4Image);
            Utils.setVisibilityVisible(holder.messageStatusText);

            if (message.isServered()) {
                holder.statusSentImage.setImageResource(R.drawable.ic_check_done);
                holder.statusDeliveredImage.setImageResource(R.drawable.ic_check_done);

                holder.messageStatusText.setText(R.string.chat_message_status_delivered);
            } else {
                holder.statusSentImage.setImageResource(R.drawable.ic_check_done);
                holder.statusDeliveredImage.setImageResource(R.drawable.ic_check);

                holder.messageStatusText.setText(R.string.chat_message_status_sent);
            }
        } else {
            Utils.setVisibilityGone(holder.statusSentImage);
            Utils.setVisibilityGone(holder.statusDeliveredImage);
            Utils.setVisibilityGone(holder.status3Image);
            Utils.setVisibilityGone(holder.status4Image);
            Utils.setVisibilityGone(holder.messageStatusText);
        }

        bindSelectableView(holder, holder.itemView, holder.deleteCheck, item);

        holder.itemView.setOnLongClickListener(v -> {
            if (!mSelectingState) {
                MessageItem messageItem = getItem(holder.getAdapterPosition());

                StringSet selectableMessages = new StringSet();
                BusinessLogic.GetInstance().GetEditableMessageIdsForChat(mChatId, selectableMessages);
                String messageId = messageItem.getMessage().getId();

                mSelectionListener.onMessageSelected(messageItem, selectableMessages.has_key(messageId), messageItem.getMessage().isTextMessage() && BusinessLogic.GetInstance().CanEditMessage(messageId), messageItem.getMessage().isTextMessage());
                return true;
            }
            return false;
        });

        if (previousIsSame) {
            holder.contactAvatar.setVisibility(View.INVISIBLE);
        } else {
            holder.contactAvatar.setVisibility(View.VISIBLE);
            bindBase(message.getSender(), holder.contactAvatar);
        }

        holder.contactIsDodicall.setVisibility(previousIsSame
                ? View.INVISIBLE
                : View.VISIBLE);

        holder.messageContainer.setBackgroundResource(previousIsSame
                ? R.drawable.right_bubble
                : R.drawable.right_bubble_appendix);

        if (messages != null) {
            boolean withoutContent = TextUtils.isEmpty(message.getContent());
            holder.replyContainer.setBackgroundResource(previousIsSame
                    ? withoutContent ? R.drawable.bubble_citation : R.drawable.bubble_citation_w_answer
                    : withoutContent ? R.drawable.right_bubble_appendix_only_citation : R.drawable.right_bubble_appendix_citation);
        }

        holder.itemView.setPadding(
                mPadding,
                previousIsSame ? 0 : mPadding,
                mPadding,
                mPadding);
    }

    private void bindRemovedMyMessage(ChatMessage message, BaseMessageViewHolder holder, boolean previousIsSame) {
        if (previousIsSame) {
            holder.contactAvatar.setVisibility(View.INVISIBLE);
        } else {
            holder.contactAvatar.setVisibility(View.VISIBLE);
            bindBase(message.getSender(), holder.contactAvatar);
        }

        holder.contactIsDodicall.setVisibility(previousIsSame
                ? View.INVISIBLE
                : View.VISIBLE);

        holder.messageContainer.setBackgroundResource(previousIsSame
                ? R.drawable.right_bubble
                : R.drawable.right_bubble_appendix);

        holder.itemView.setPadding(
                mPadding,
                previousIsSame ? 0 : mPadding,
                mPadding,
                mPadding);
    }

    private void bindBase(final Contact contact, final RoundedImageView avatarImage) {
        Picasso.with(getContext())
                .load(new File(contact.avatarPath))
                .networkPolicy(NetworkPolicy.NO_STORE)
                .transform(ROUNDED_TRANSFORMATION)
                .placeholder(R.drawable.no_photo_user)
                .into(avatarImage);
    }

    private void bindCommon(final ChatMessage message, final BaseMessageViewHolder holder, final TextView messageTimeTextView, final View editMessageView) {
        messageTimeTextView.setText(Utils.formatTime(message.getSendTime()));

        String content = message.getContent();

        if (message.isEncrypted()) {
            content = getContext().getString(R.string.chat_message_encrypted);
        }

        TextView messageTextView = holder.messageText;
        if (!TextUtils.isEmpty(content)) {
            Spannable spannable = new SpannableString(content);

            for (int i = 0, n = 0; i < content.length(); ++i) {
                if (content.charAt(i) == ' ') {
                    String word = content.substring(n, i);

                    matchAndSetUrlSpan(spannable, word, n);

                    n = i + 1;
                } else if (i == content.length() - 1) {
                    String word = content.substring(n);
                    matchAndSetUrlSpan(spannable, word, n);
                }
            }

            messageTextView.setVisibility(View.VISIBLE);
            messageTextView.setMovementMethod(LongClickableMovementMethod.getInstance(getContext()));
            messageTextView.setText(spannable);
        } else {
            messageTextView.setVisibility(View.GONE);
        }

        editMessageView.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

        if (message.isContactMessage()) {
            messageTextView.setVisibility(View.GONE);
            holder.contactLayout.setVisibility(View.VISIBLE);
            holder.sharedContactName.setText(Utils.formatAccountFullName(message.getSharedContact()));
            if (!TextUtils.isEmpty(message.getSharedContact().avatarPath)) {
                bindBase(message.getSharedContact(), holder.sharedContactAvatar);
            } else {
                holder.sharedContactAvatar.setUrl(null);
            }

            OnContactClickListener contactClickListener = new OnContactClickListener(message.getSharedContact(), mSelectionListener);

            holder.sharedContactName.setOnClickListener(contactClickListener);
            holder.sharedContactAvatar.setOnClickListener(contactClickListener);
            holder.contactLayout.setOnClickListener(contactClickListener);
            holder.itemView.setOnClickListener(contactClickListener);
        } else {
            holder.contactLayout.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }
    }

    private void matchAndSetUrlSpan(Spannable spannable, String url, int start) {
        if (url != null && url.length() >= 4) {
            String _url = url.toLowerCase();
            if (_url.startsWith("http://") || _url.startsWith("https://")) {
                spannable.setSpan(new URLSpan(_url), start, start + _url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (_url.startsWith("www.")) {
                spannable.setSpan(new URLSpan("http://" + _url), start, start + _url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void quoteMessages(LinearLayout linearLayout, List<ChatMessage> chatMessages, int level) {
        if (level < MAX_QUOTE_LEVEL) {
            linearLayout.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            for (ChatMessage chatMessage : chatMessages) {
                View view = layoutInflater.inflate(R.layout.chat_message_quote_layout, linearLayout, false);
                TextView replyText = (TextView) view.findViewById(R.id.reply_text);
                ((TextView) view.findViewById(R.id.reply_user)).setText(Utils.formatAccountFullName(chatMessage.getSender()));
                ((TextView) view.findViewById(R.id.reply_time)).setText(Utils.formatTime(chatMessage.getSendTime()));
                replyText.setText(chatMessage.getContent());
                replyText.setVisibility(!TextUtils.isEmpty(chatMessage.getContent()) ? View.VISIBLE : View.GONE);
                if (chatMessage.getQuotedMessages() != null) {
                    LinearLayout subMessages = (LinearLayout) view.findViewById(R.id.quoted_message_container);
                    quoteMessages(subMessages, chatMessage.getQuotedMessages(), level + 1);
                }
                linearLayout.addView(view);
            }
        }
    }

    public void setSelecting(boolean selecting) {
        setSelecting(selecting, 0);
    }

    public void setSelecting(boolean selecting, int counter) {
        mSelectingState = selecting;
        if (!selecting) {
            for (MessageItem message : getData()) {
                message.setSelected(false);
            }
        }
        changeCount(counter);
        notifyDataSetChanged();
    }

    public void selectAll(boolean select) {
        int myMessagesCount = 0;
        for (int i = 0; i < getItemCount(); i++) {
            int itemType = getItemViewType(i);
            if (itemType == TYPE_MESSAGE_MY || getItemViewType(i) == TYPE_MESSAGE) {
                MessageItem message = getItem(i);
                message.setSelected(select);
                myMessagesCount++;
            }
        }
        notifyDataSetChanged();
        changeCount(select ? myMessagesCount : 0);
    }

    private void changeCount(int messagesCounter) {
        mSelectedMessagesCounter = messagesCounter;
        mSelectionListener.onMessageSelected(mSelectedMessagesCounter);
    }

    class BaseMessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.contact_avatar)
        RoundedImageView contactAvatar;

        @BindView(R.id.contact_is_dodicall)
        ImageView contactIsDodicall;

        @BindView(R.id.message_text)
        TextView messageText;

        @BindView(R.id.message_container)
        View messageContainer;

        @BindView(R.id.contact_layout)
        @Nullable
        View contactLayout;

        @BindView(R.id.shared_contact_avatar)
        @Nullable
        RoundedImageView sharedContactAvatar;

        @BindView(R.id.shared_contact_name)
        @Nullable
        TextView sharedContactName;


        private BaseMessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            messageText.setTextSize(mFontSize);
        }
    }

    class BaseForeignMessageViewHolder extends BaseMessageViewHolder {
        @BindView(R.id.message_account_text)
        TextView messageAccountText;

        @BindView(R.id.contact_is_dodicall)
        ImageView contactIsDodicall;

        private BaseForeignMessageViewHolder(View itemView) {
            super(itemView);

            messageAccountText.setVisibility(mIsP2PChat ? View.GONE : View.VISIBLE);
        }
    }

    class RemovedMessageViewHolder extends BaseForeignMessageViewHolder {

        private RemovedMessageViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MessageViewHolder extends BaseForeignMessageViewHolder {
        @BindView(R.id.message_time_text)
        TextView messageTimeText;

        @BindView(R.id.message_edited)
        View messageEditView;

        @BindView(R.id.reply_container)
        LinearLayout replyContainer;

        @BindView(R.id.delete_check)
        CheckBox deleteCheck;

        private MessageViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MyRemovedMessageViewHolder extends BaseMessageViewHolder {
        private MyRemovedMessageViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MyMessageViewHolder extends BaseMessageViewHolder {
        @BindView(R.id.message_time_text)
        TextView messageTimeText;

        @BindView(R.id.status_sent_image)
        ImageView statusSentImage;

        @BindView(R.id.message_container)
        ViewGroup messageContainer;

        @BindView(R.id.status_delivered_image)
        ImageView statusDeliveredImage;

        @BindView(R.id.status_3_image)
        ImageView status3Image;

        @BindView(R.id.status_4_image)
        ImageView status4Image;

        @BindView(R.id.message_status_text)
        TextView messageStatusText;

        @BindView(R.id.delete_check)
        CheckBox deleteCheck;

        @BindView(R.id.message_edited)
        View messageEditView;

        @BindView(R.id.reply_container)
        LinearLayout replyContainer;

        private MyMessageViewHolder(View itemView) {
            super(itemView);

            replyContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                messageEditView.setTranslationY(bottom);
            });
        }
    }

    class NotificationMessageViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.message_text)
        TextView messageText;

        private NotificationMessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            messageText.setTextSize(mFontSize);
        }
    }

    class DateSeparatorViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.date_text)
        TextView dateText;

        private DateSeparatorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            dateText.setTextSize(mFontSize);
        }
    }

    class UnreadSeparatorViewHolder extends RecyclerView.ViewHolder {
        private UnreadSeparatorViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface SelectionListener {
        void onMessageSelected(MessageItem messageItem, boolean canBeDeleted, boolean canBeEdited, boolean canBeCopied);

        void onContactSelected(Contact contact);

        void onMessageSelected(int selectedCount);
    }
}
