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

import android.os.AsyncTask;

import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.ContactsManager;

public class ContactActionAsyncTask extends AsyncTask<Void, Void, Contact> {

    private ContactsManager mContactsManager;
    private ContactAction mContactAction;
    private Contact mContact;

    private ContactActionListener mContactActionListener;

    private ContactActionAsyncTask(ContactsManager contactsManager, ContactAction contactAction, Contact contact, ContactActionListener contactActionListener) {
        mContactsManager = contactsManager;
        mContactAction = contactAction;
        mContact = contact;
        mContactActionListener = contactActionListener;
    }

    @Override
    protected Contact doInBackground(Void[] params) {
        if (mContactAction == ContactAction.SaveToContacts) {
            return mContactsManager.saveContact(mContact, mContact.getId());
        } else if (mContactAction == ContactAction.AcceptRequest) {
            return mContactsManager.acceptRequest(mContact);
        } else if (mContactAction == ContactAction.DeclineRequest) {
            return mContactsManager.declineContact(mContact);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Contact contact) {
        if (contact == null) {
            mContactActionListener.onContactActionFail();
        } else {
            mContactActionListener.onContactActionSuccess(contact);
        }
    }

    public static ContactActionAsyncTask getSaveAsyncTask(ContactsManager contactsManager, Contact contact, ContactActionListener contactActionListener) {
        return new ContactActionAsyncTask(contactsManager, ContactAction.SaveToContacts, contact, contactActionListener);
    }

    public static ContactActionAsyncTask getDeclineAsyncTask(ContactsManager contactsManager, Contact contact, ContactActionListener contactActionListener) {
        return new ContactActionAsyncTask(contactsManager, ContactAction.DeclineRequest, contact, contactActionListener);
    }

    public static ContactActionAsyncTask getAcceptAsyncTask(ContactsManager contactsManager, Contact contact, ContactActionListener contactActionListener) {
        return new ContactActionAsyncTask(contactsManager, ContactAction.AcceptRequest, contact, contactActionListener);
    }

    private enum ContactAction {
        SaveToContacts, AcceptRequest, DeclineRequest
    }

    public interface ContactActionListener {
        void onContactActionSuccess(Contact contact);

        void onContactActionFail();
    }
}
