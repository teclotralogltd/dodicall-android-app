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

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.bl.CallHistoriesList;
import ru.swisstok.dodicall.manager.BusinessLogicCallback;
import ru.swisstok.dodicall.manager.NotificationManager;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.NotificationsUtils;

public class NotificationManagerImpl extends BaseManagerImpl implements NotificationManager {

    private Pair<NotificationsUtils.NotificationType, Object> mCurrentActiveScreen;

    @Override
    public void setCurrentActivity(NotificationsUtils.NotificationType notificationType, String activityId, boolean withNotificationUpdate) {
        mCurrentActiveScreen = Pair.create(notificationType, activityId);
        if (withNotificationUpdate) {
            updateNotificationByType(notificationType, activityId);
        }
    }

    @Override
    public void clearCurrentActivity() {
        mCurrentActiveScreen = null;
    }

    @Override
    public void clearCache() {
        clearCurrentActivity();
    }

    @Override
    public void onCallback(BusinessLogicCallback.Event event, ArrayList<String> ids) {
        if (event == BusinessLogicCallback.Event.ContactSubscriptions) {
            updateInviteNotification();
        } else if (event == BusinessLogicCallback.Event.Chats) {
            updateChatNotification(false, ids.toArray(new String[ids.size()]));
        }
    }

    private void updateNotificationByType(@Nullable NotificationsUtils.NotificationType notificationType, String... ids) {
        if (notificationType != null) {
            NotificationsUtils.cancelNotification(getContext(), notificationType);

            if (notificationType == NotificationsUtils.NotificationType.Chat) {
                updateChatNotification(true, ids);
            } else if (notificationType == NotificationsUtils.NotificationType.MissedCall) {
                updateHistoryNotification();
            } else if (notificationType == NotificationsUtils.NotificationType.Invite) {
                updateInviteNotification();
            }
        }
    }

    private void updateChatNotification(boolean withUpdate, String... ids) {
        List<Chat> chats = ChatManagerImpl.getInstance().getChats();

        if (CollectionUtils.isEmpty(chats)) {
            return;
        }

        NotificationsUtils.NotificationType notificationType = null;
        String chatId = null;
        if (mCurrentActiveScreen != null) {
            notificationType = mCurrentActiveScreen.first;
            if (notificationType == NotificationsUtils.NotificationType.Chat) {
                chatId = (String) mCurrentActiveScreen.second;
            }
        }

        for (int i = chats.size() - 1; i >= 0; i--) {
            Chat c = chats.get(i);
            if (c.getNewMessagesCount() == 0) {
                chats.remove(i);
            } else if (c.getId().equals(chatId)) {
                chats.remove(i); // activity for specified chat exists
            } else if (c.getLastMessage().isDeletedMessage()) {
                chats.remove(i);
            }
        }

        if (chats.isEmpty()) {
            NotificationsUtils.cancelNotification(getContext(), NotificationsUtils.NotificationType.Chat);
            return;
        }

        boolean withHeadsUp = true;
        if (chatId != null && ids.length == 1) {
            String updatedChatId = ids[0];
            if (updatedChatId != null && chatId.equals(updatedChatId)) {  //if updated chats contains ONLY current open chat - do not show heads-up
                withHeadsUp = false;
            }
        }

        if (!(withHeadsUp || withUpdate)) {
            return;
        }

        if (notificationType == null || //current activity is not trackable
                notificationType != NotificationsUtils.NotificationType.Chat || //current activity is not chat activity
                chatId != null) { //current activity is chat but chat is specified
            int ringerMode = getAudioManager(getContext()).getRingerMode();
            NotificationsUtils.createChatNotification(getContext(), chats, withHeadsUp, ringerMode != AudioManager.RINGER_MODE_SILENT, ringerMode == AudioManager.RINGER_MODE_NORMAL);
        }
    }

    private void updateHistoryNotification() {
        if (getContext() == null) {
            return;
        }

        NotificationsUtils.NotificationType notificationType = null;
        String historyId = null;
        if (mCurrentActiveScreen != null) {
            notificationType = mCurrentActiveScreen.first;
            if (notificationType == NotificationsUtils.NotificationType.MissedCall) {
                historyId = (String) mCurrentActiveScreen.second;
            }
        }

        CallHistoriesList list = BL.getCallHistoriesList();

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            CallHistory c = list.get(i);
            if (c.id.equals(historyId)) {
                list.remove(i); // activity for specified history exists
                break;
            }
        }

        if (notificationType == null || //current activity is not trackable
                notificationType != NotificationsUtils.NotificationType.MissedCall || //current activity is not history activity
                historyId != null) { //current activity is history but user is specified
            NotificationsUtils.createHistoryNotification(getContext(), list, getAudioManager(getContext()).getRingerMode() != AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void updateInviteNotification() {
        if (getContext() == null) {
            return;
        }

        if (mCurrentActiveScreen != null && mCurrentActiveScreen.first == NotificationsUtils.NotificationType.Invite) {
            return;
        }

        List<Contact> list = ContactsManagerImpl.getInstance().getNewInvites();

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        int ringerMode = getAudioManager(getContext()).getRingerMode();
        NotificationsUtils.createInviteNotification(getContext(), list, ringerMode != AudioManager.RINGER_MODE_SILENT, ringerMode == AudioManager.RINGER_MODE_NORMAL);
    }

    private static AudioManager getAudioManager(Context context) {
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }
}
