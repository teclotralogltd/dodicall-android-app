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

package ru.swisstok.dodicall.manager.impl;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.BusinessLogicCallback;
import ru.swisstok.dodicall.manager.ChatManager;
import ru.swisstok.dodicall.util.ChatComparator;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.Logger;

public class ChatManagerImpl extends BaseManagerImpl implements ChatManager {

    private static final ChatComparator CHAT_COMPARATOR = new ChatComparator();

    private ConcurrentHashMap<String, Chat> mChats;
    private Map<String, String> mIncompleteMessageMap;
    private final Object mChatAccessLock = new Object();

    private static volatile ChatManager sInstance;

    private ChatManagerImpl() {
        mChats = new ConcurrentHashMap<>();
        mIncompleteMessageMap = new TreeMap<>();
    }

    public static ChatManager getInstance() {
        if (sInstance == null) {
            synchronized (ChatManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new ChatManagerImpl();
                }
            }
        }
        return sInstance;
    }

    @Override
    public List<Chat> getChats() {
        Logger.onOperationStart("LoadChatsProvider");

        prepareChatsContainer();

        List<Chat> result = new ArrayList<>();
        synchronized (mChatAccessLock) {
            for (Chat chat : mChats.values()) {
                ChatMessage lastMessage = chat.getLastMessage();
                String incompleteMessage = mIncompleteMessageMap.get(chat.getId());
                if ((lastMessage != null) || !TextUtils.isEmpty(incompleteMessage)) {
                    chat.setIncompleteMessage(incompleteMessage);
                    result.add(chat);
                }
            }
        }
        Logger.onOperationEnd("LoadChatsProvider");

        Collections.sort(result, CHAT_COMPARATOR);
        return result;
    }

    @Override
    public Chat getChat(String chatId) {
        prepareChatsContainer();

        synchronized (mChatAccessLock) {
            Chat chatModel = mChats.get(chatId);

            if (chatModel == null) {
                chatModel = BL.getChatById(chatId);

                if (chatModel != null) {
                    mChats.put(chatModel.getId(), chatModel);
                }
            }
            if (chatModel != null) {
                chatModel.setIncompleteMessage(mIncompleteMessageMap.get(chatId));
            }

            return chatModel;
        }
    }

    @Override
    public Chat createChat(String[] ids) {
        Chat chat = BL.createChatWithContacts(ids);
        if (chat != null) {
            prepareChatsContainer();

            mChats.put(chat.getId(), chat);
        }
        return chat;
    }

    @Override
    public void updateChatMembers() {

    }

    @Override
    public void readChatMessage(String chatId, String messageId) {
        if (BL.readChatMessage(messageId)) {
            prepareChatsContainer();

            synchronized (mChatAccessLock) {
                Chat chat = BL.getChatById(chatId);
                if (chat != null) {
                    mChats.put(chatId, chat);
                    notifySubscribers(CHATS_UPDATED, chatId);
                }
            }
        }
    }

    @Override
    public void updateIncompleteMessage(String chatId, String incompleteMessage) {
        mIncompleteMessageMap.put(chatId, incompleteMessage);
        notifySubscribers(CHATS_UPDATED, chatId);
    }

    @Override
    public void removeChat(String chatId) {
        mChats.remove(chatId);
    }

    @Override
    public void clearCache() {
        mChats.clear();
        mIncompleteMessageMap.clear();
    }

    @Override
    public void onCallback(BusinessLogicCallback.Event event, ArrayList<String> ids) {
        if (event == BusinessLogicCallback.Event.ContactsPresence) {
            if (CollectionUtils.isNotEmpty(ids)) {
                if (!prepareChatsContainer()) {
                    synchronized (mChatAccessLock) {
                        for (Chat chat : BL.getChats()) {
                            mChats.put(chat.getId(), chat);
                        }
                    }
                }
            }
        } else if (event == BusinessLogicCallback.Event.Chats) {
            updateChats(ids);
        } else if (event == BusinessLogicCallback.Event.ChatMessages) {
            notifySubscribers(CHAT_MESSAGES_UPDATED, ids);
        }
    }

    private boolean prepareChatsContainer() {
        if (mChats == null) {
            synchronized (mChatAccessLock) {
                if (mChats == null) {
                    mChats = new ConcurrentHashMap<>();

                    for (Chat chat : BL.getChats()) {
                        mChats.put(chat.getId(), chat);
//                        extractChatContacts(chat);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void updateChats(List<String> ids) {
        if (mChats != null) {
            ArrayList<String> updatedIdsList = new ArrayList<>();
            ArrayList<String> deletedIdsList = new ArrayList<>();

            synchronized (mChatAccessLock) {
                TreeSet<String> ts = new TreeSet<>(ids);

                ArrayList<Chat> chats = BL.getChats(ids.toArray(new String[ids.size()]));
                if (!chats.isEmpty()) {
                    for (Chat chat : chats) {
                        final String chatId = chat.getId();

                        mChats.put(chatId, chat);
//                        extractChatContacts(chat);
                        ts.remove(chatId);

                        updatedIdsList.add(chatId);
                    }

                    for (String deletedChatId : ts) {
                        mChats.remove(deletedChatId);
                        deletedIdsList.add(deletedChatId);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(updatedIdsList)) {
                notifySubscribers(CHATS_UPDATED, updatedIdsList);
            }

            if (CollectionUtils.isNotEmpty(deletedIdsList)) {
                notifySubscribers(CHATS_REMOVED, deletedIdsList);
            }
        }
    }
}
