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

package ru.swisstok.dodicall.manager;

import java.util.List;

import ru.swisstok.dodicall.api.Chat;

public interface ChatManager extends BaseManager {

    String CHATS_UPDATED = "ru.swisstok.action.ChatsUpdated";
    String CHATS_RECREATED = "ru.swisstok.action.ChatsRecreated";
    String CHATS_REMOVED = "ru.swisstok.action.ChatsRemoved";
    public static final String CHAT_MESSAGES_UPDATED = "ru.swisstok.action.ChatMessagesUpdated";

    List<Chat> getChats();

    Chat getChat(String id);

    Chat createChat(String[] ids);

    void updateChatMembers();

    void readChatMessage(String chatId, String messageId);

    void updateIncompleteMessage(String chatId, String incompleteMessage);

    void removeChat(String chatId);
}
