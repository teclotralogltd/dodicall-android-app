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
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.joda.time.DateTimeComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.MessageItem;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.ChatMessageComparator;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.Logger;

public class ChatMessagesTaskLoader extends ContentResolverAsyncTaskLoader<ArrayList<MessageItem>> {

    private static final ChatMessageComparator CHAT_MESSAGE_COMPARATOR = new ChatMessageComparator();
    private static final DateTimeComparator COMPARATOR = DateTimeComparator.getDateOnlyInstance();

    private final String mChatId;
    private final String mFirstLoadedMessageId;
    private final String[] mIds;
    private final ArrayList<MessageItem> mMessageItems;

    public ChatMessagesTaskLoader(Context context, String chatId, String firstLoadedMessageId, String[] ids, ArrayList<MessageItem> messageItems) {
        super(context);
        mChatId = chatId;
        mIds = ids;
        mFirstLoadedMessageId = firstLoadedMessageId;
        mMessageItems = messageItems;
    }

    @Nullable
    @Override
    public ArrayList<MessageItem> loadInBackground(@NonNull ContentResolver contentResolver) {
        final ArrayList<MessageItem> messageItems = mMessageItems;
        Logger.onOperationStart("LoadChatMessagesProvider");
        ArrayList<ChatMessage> newMessages;
        if (mIds == null) {
            newMessages = BL.getChatMessagesPaged(mChatId, mFirstLoadedMessageId);
        } else {
            newMessages = BL.getChatMessages(mChatId, mIds);
        }
        Logger.onOperationEnd("LoadChatMessagesProvider", newMessages.size());
        ArrayList<MessageItem> newItems = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(newMessages)) {
            Collections.sort(newMessages, CHAT_MESSAGE_COMPARATOR);
            newItems.addAll(Stream.of(newMessages).map(MessageItem::new).collect(Collectors.toList()));

            for (Iterator<MessageItem> iterator = newItems.iterator(); iterator.hasNext(); ) {
                MessageItem cm = iterator.next();
                for (int i = 0; i < messageItems.size(); ++i) {
                    MessageItem mi = messageItems.get(i);
                    if (mi.getMessage() != null && TextUtils.equals(cm.getMessage().getId(), mi.getMessage().getId())) {
                        messageItems.set(i, cm);
                        iterator.remove();
                    }
                }
                if (cm.getMessage().isContactMessage()) {
                    ContactsManagerImpl.getInstance().addOtherContact(cm.getMessage().getSharedContact());
//                    contentResolver.insert(DataProvider.makeChatContactsUri(mChatId), cm.getMessage().getSharedContact().toContentValues());
                }
            }
            for (int i = messageItems.size() - 1; i >= 0; i--) {
                MessageItem mi = messageItems.get(i);
                if (mi.getMessage() == null) {
                    messageItems.remove(i);
                }
            }

            if (mIds == null) {
                messageItems.addAll(0, newItems);
            } else {
                messageItems.addAll(newItems);
            }

            insertDateSeparators(messageItems, 0);

            if (mIds == null) {
                for (int i = messageItems.size() - 1; i >= 0; --i) {
                    MessageItem item = messageItems.get(i);

                    if (item.getMessage() != null && item.getMessage().isRead()) {
                        if (i < messageItems.size() - 1) {
                            messageItems.add(i + 1, new MessageItem(true));
                        }

                        break;
                    }
                    if (i == 1 && item.getMessage() != null && !item.getMessage().isRead()) {
                        messageItems.add(1, new MessageItem(true));
                    }
                }
            }

            MessageItem lastItemWithStatusSent = null;
            MessageItem lastItemWithStatusDelivered = null;

            for (MessageItem mi : messageItems) {
                mi.setShowStatus(false);
                if (mi.getMessage() != null && mi.getMessage().getSender().iAm) {
                    if (mi.getMessage().isServered()) {
                        if (lastItemWithStatusDelivered != null) {
                            lastItemWithStatusDelivered.setShowStatus(false);
                        }

                        lastItemWithStatusDelivered = mi;
                    } else {
                        if (lastItemWithStatusSent != null) {
                            lastItemWithStatusSent.setShowStatus(false);
                        }

                        lastItemWithStatusSent = mi;
                    }

                    mi.setShowStatus(true);
                }
            }
        }

        return newItems;
    }

    private void insertDateSeparators(ArrayList<MessageItem> list, int from) {
        long date = list.get(from).getMessage().getSendTime();

        list.add(from, new MessageItem(date));

        for (int i = from + 2; i < list.size(); ++i) {
            long sendDateTime = list.get(i).getMessage().getSendTime();

            if (COMPARATOR.compare(date, sendDateTime) != 0) {
                insertDateSeparators(list, i);
                break;
            }
        }
    }
}
