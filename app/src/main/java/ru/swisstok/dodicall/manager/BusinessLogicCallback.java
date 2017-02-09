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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.uls_global.dodicall.CallbackFunction;
import ru.uls_global.dodicall.StringList;

public class BusinessLogicCallback extends CallbackFunction {

    public enum Event {
        Contacts, ContactsPresence, ContactSubscriptions,
        Chats, ChatMessages,
        Calls,
        NetworkStateChanged, History, Logout, LoggedIn,
        AccountDataUpdated, DeviceSettingsUpdated, UserSettingsChanged, PresenceOffline,
        SecretKey
    }

    private List<BaseManager> mManagers;

    public BusinessLogicCallback() {
        mManagers = new ArrayList<>();
    }

    public void addManager(BaseManager... managers) {
        Collections.addAll(mManagers, managers);
    }

    @Override
    public void run(String modelName, StringList entityIds) {
        Event event = null;
        for (Event existingEvent : Event.values()) {
            if (existingEvent.name().equals(modelName)) {
                event = existingEvent;
                break;
            }
        }
        if (event == null) {
            return;
        }
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < entityIds.size(); i++) {
            ids.add(entityIds.get(i));
        }

        for (BaseManager manager : mManagers) {
            manager.onCallback(event, ids);
        }
    }
}
