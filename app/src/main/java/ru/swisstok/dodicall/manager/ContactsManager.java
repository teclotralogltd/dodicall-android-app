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

import android.support.annotation.Nullable;

import java.util.List;

import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.Contact;

public interface ContactsManager extends BaseManager {

    String CONTACTS_UPDATED = "ContactsUpdated";
    String CONTACTS_ACCEPTED = "ContactsAccepted";
    String CONTACT_REMOVED = "ContactRemoved";
    String INVITE_READ = "InviteRead";
    String ACTION_USERS_STATUSES_UPDATED = "UserStatusUpdated";

    int FILTER_ALL = 0;    
    int FILTER_DDC = 1;
    int FILTER_PHONE = 2;
    int FILTER_SAVED = 3;
    int FILTER_BLOCKED = 4;
    int FILTER_WHITE = 5;

    List<Contact> getContacts(int filter, @Nullable String selection, boolean withRequests, boolean withInvites);

    Contact getContactById(int id);

    Contact getMyContact();

    Balance getBalance();

    void updateBalance(Balance balance);

    List<Contact> getNewInvites();

    Contact saveContact(Contact contact, int currentId);

    void deleteContact(Contact contact);

    Contact blockContact(Contact contact, boolean blocked);

    Contact addToWhiteList(Contact contact, boolean white);

    Contact declineContact(Contact contact);

    Contact acceptRequest(Contact contact);

    void readInvite(Contact contact);

    Contact updateFavorite(Contact contact, String sip);

    void cleanNewAccepts();

    void addOtherContact(Contact contact);
}
