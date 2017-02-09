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

package ru.swisstok.dodicall.api;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import org.parceler.Parcel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.ContactModel;
import ru.uls_global.dodicall.ContactPresenceStatusModel;
import ru.uls_global.dodicall.ContactSubscriptionState;
import ru.uls_global.dodicall.ContactsContactList;
import ru.uls_global.dodicall.ContactsContactModel;
import ru.uls_global.dodicall.ContactsContactType;

@Parcel
public class Contact implements Serializable, DbStruct {
    public static final int SUBSCRIPTION_STATE_NONE = ContactSubscriptionState.ContactSubscriptionStateNone.swigValue();
    public static final int SUBSCRIPTION_STATE_FROM = ContactSubscriptionState.ContactSubscriptionStateFrom.swigValue();
    public static final int SUBSCRIPTION_STATE_TO = ContactSubscriptionState.ContactSubscriptionStateTo.swigValue();
    public static final int SUBSCRIPTION_STATE_BOTH = ContactSubscriptionState.ContactSubscriptionStateBoth.swigValue();

    public int id;
    public String dodicallId;
    public String phonebookId;
    public String nativeId;
    public String xmppId;
    public String firstName;
    public String lastName;
    public String middleName;
    public String avatarPath;
    public boolean blocked;
    public boolean white;
    public boolean invite;
    public boolean subscriptionRequest;
    public boolean newInvite;
    public boolean directory;
    public boolean iAm;
    public boolean isMine;
    public boolean isDeclinedRequest;
    public List<String> phones;
    public List<String> sips;
    public int subscriptionState;
    public ContactStatus contactStatus;
    public boolean newlyAcceptedInvite;

    public Contact() {
    }

    public Contact(
            int id, String dodicallId, String phonebookId,
            String nativeId, String xmppId, String firstName, String lastName,
            String middleName, String avatarPath, boolean blocked, boolean white,
            int baseStatus, String extraStatus, boolean invite,
            boolean subscriptionRequest, boolean newInvite, boolean directory,
            boolean iAm, boolean isDeclinedRequest,
            ArrayList<String> phones, ArrayList<String> sips, int subscriptionState) {
        this.id = id;
        this.dodicallId = dodicallId;
        this.phonebookId = phonebookId;
        this.nativeId = nativeId;
        this.xmppId = xmppId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.avatarPath = avatarPath;
        this.blocked = blocked;
        this.white = white;
        this.contactStatus = new ContactStatus(xmppId, baseStatus, extraStatus);
        this.invite = invite;
        this.subscriptionRequest = subscriptionRequest;
        this.newInvite = newInvite;
        this.directory = directory;
        this.iAm = iAm;
        this.isDeclinedRequest = isDeclinedRequest;
        this.phones = phones;
        this.sips = sips;
        this.subscriptionState = subscriptionState;
    }

    public Contact(Cursor cursor) {
        id = cursor.getInt(cursor.getColumnIndex(DataProvider.ContactsColumn.ID));
        dodicallId = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.DODICALL_ID)
        );
        phonebookId = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.PHONEBOOK_ID)
        );
        nativeId = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.NATIVE_ID)
        );
        xmppId = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.XMPP_ID)
        );
        firstName = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.FIRSTNAME)
        );
        lastName = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.LASTNAME)
        );
        middleName = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.MIDDLENAME)
        );
        avatarPath = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.AVATAR_PATH)
        );
        blocked = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.BLOCKED)
        ) == 1;
        white = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.WHITE)
        ) == 1;
        int baseStatus = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.BASE_STATUS)
        );
        String extraStatus = cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.EXTRA_STATUS)
        );
        contactStatus = new ContactStatus(xmppId, baseStatus, extraStatus);
        invite = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.INVITE)
        ) == 1;
        subscriptionRequest = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.SUBSCRIPTION_REQUEST)
        ) == 1;
        newInvite = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.NEW_INVITE)
        ) == 1;
        directory = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.DIRECTORY)
        ) == 1;
        iAm = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.I_AM)
        ) == 1;
        isMine = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.IS_MINE)
        ) == 1;
        isDeclinedRequest = cursor.getInt(
                cursor.getColumnIndex(DataProvider.ContactsColumn.DECLINED_REQUEST)
        ) == 1;
        sips = getPhonesFromString(cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.SIP_LIST)
        ));
        phones = getPhonesFromString(cursor.getString(
                cursor.getColumnIndex(DataProvider.ContactsColumn.PHONES_LIST)
        ));

        subscriptionState = cursor.getInt(cursor.getColumnIndex(DataProvider.ContactsColumn.SUBSCRIPTION_STATE));
    }

    public Contact(ContactModel contactModel) {
        id = contactModel.getId();
        dodicallId = contactModel.getDodicallId();
        phonebookId = contactModel.getPhonebookId();
        nativeId = contactModel.getNativeId();
        xmppId = contactModel.GetXmppId();
        firstName = contactModel.getFirstName();
        lastName = contactModel.getLastName();
        middleName = contactModel.getMiddleName();
        avatarPath = contactModel.getAvatarPath();
        blocked = contactModel.getBlocked();
        white = contactModel.getWhite();
        final ContactPresenceStatusModel statusModel = DataProvider.getUserStatus(xmppId);
        contactStatus = new ContactStatus(xmppId, statusModel.getBaseStatus().swigValue(), statusModel.getExtStatus());
        invite = DataProvider.isInvite(contactModel);
        subscriptionRequest = contactModel.getSubscription().getAskForSubscription();
        newInvite = DataProvider.isNewInvite(contactModel);
        directory = DataProvider.isDirectory(contactModel);
        iAm = contactModel.getIam();
        isDeclinedRequest = DataProvider.isDeclinedRequest(contactModel);
        final ContactsContactList contacts = BusinessLogic.GetInstance().GetContacts(contactModel);
        phones = getPhoneNumbers(contacts);
        sips = getSipNumbers(contacts);
        subscriptionState = contactModel.getSubscription().getSubscriptionState().swigValue();
    }

    private static List<String> getPhonesFromString(String phones) {
        return !TextUtils.isEmpty(phones) ? new ArrayList<>(Arrays.asList(phones.split(DataProvider.LIST_SEPARATOR))) : new ArrayList<>();
    }

    public static ContentValues toContentValues(
            int id, String dodicallId, String phonebookId,
            String nativeId, String xmppId, String firstName,
            String lastName, String middleName, String avatarPath, boolean blocked,
            boolean white, int baseStatus, String extraStatus,
            boolean invite, boolean subscriptionRequest, boolean newInvite,
            boolean directory, boolean iAm, boolean isMine, boolean isDeclinedRequest,
            List<String> sips, List<String> phones, int subscriptionState) {
        ContentValues cv = new ContentValues();
        cv.put(DataProvider.ContactsColumn.ID, id);
        cv.put(DataProvider.ContactsColumn.DODICALL_ID, dodicallId);
        cv.put(DataProvider.ContactsColumn.PHONEBOOK_ID, phonebookId);
        cv.put(DataProvider.ContactsColumn.NATIVE_ID, nativeId);
        cv.put(DataProvider.ContactsColumn.XMPP_ID, xmppId);
        cv.put(DataProvider.ContactsColumn.FIRSTNAME, firstName);
        cv.put(DataProvider.ContactsColumn.LASTNAME, lastName);
        cv.put(DataProvider.ContactsColumn.MIDDLENAME, middleName);
        cv.put(DataProvider.ContactsColumn.AVATAR_PATH, avatarPath);
        cv.put(DataProvider.ContactsColumn.BLOCKED, blocked);
        cv.put(DataProvider.ContactsColumn.WHITE, white);
        cv.put(DataProvider.ContactsColumn.BASE_STATUS, baseStatus);
        cv.put(DataProvider.ContactsColumn.EXTRA_STATUS, extraStatus);
        cv.put(DataProvider.ContactsColumn.INVITE, invite);
        cv.put(DataProvider.ContactsColumn.SUBSCRIPTION_REQUEST, subscriptionRequest);
        cv.put(DataProvider.ContactsColumn.NEW_INVITE, newInvite);
        cv.put(DataProvider.ContactsColumn.DIRECTORY, directory);
        cv.put(DataProvider.ContactsColumn.I_AM, iAm);
        cv.put(DataProvider.ContactsColumn.IS_MINE, isMine);
        cv.put(DataProvider.ContactsColumn.DECLINED_REQUEST, isDeclinedRequest);
        cv.put(
                DataProvider.ContactsColumn.SIP_LIST,
                sips == null ? null : TextUtils.join(DataProvider.LIST_SEPARATOR, sips)
        );
        cv.put(
                DataProvider.ContactsColumn.PHONES_LIST,
                TextUtils.join(DataProvider.LIST_SEPARATOR, phones)
        );

        cv.put(DataProvider.ContactsColumn.SUBSCRIPTION_STATE, subscriptionState);
        return cv;
    }

    @Override
    public ContentValues toContentValues() {
        return toContentValues(
                id, dodicallId, phonebookId,
                nativeId, xmppId, firstName, lastName,
                middleName, avatarPath, blocked, white,
                getStatus(), getExtraStatus(), invite,
                subscriptionRequest, newInvite, directory,
                iAm, isMine, isDeclinedRequest,
                sips, phones, subscriptionState
        );
    }

    public boolean isDodicall() {
        return !TextUtils.isEmpty(dodicallId);
    }

    public boolean isNotDodicall() {
        return !isDodicall();
    }

    public boolean isSaved() {
        return !TextUtils.isEmpty(nativeId);
    }

    public boolean isFromPhonebook() {
        return !TextUtils.isEmpty(phonebookId) && !isSaved();
    }

    public boolean isRequest() {
        return subscriptionRequest;
    }

    public boolean isInvite() {
        return invite;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public Uri getUri() {
        if (isFromPhonebook()) {
            return ContentUris.withAppendedId(
                    DataProvider.CONTACTS_URI,
                    DataProvider.PHONEBOOK_MAGIC_ID + Integer.valueOf(phonebookId)
            );
        } else if (directory) {
            return Uri.withAppendedPath(
                    DataProvider.CONTACTS_DDC_SEARCH_URI, dodicallId
            );
        } else if (isInvite()) {
            return ContentUris.withAppendedId(
                    DataProvider.CONTACTS_URI,
                    Math.abs(DataProvider.INVITE_MAGIC_ID + dodicallId.hashCode())
            );
        } else {
            return ContentUris.withAppendedId(DataProvider.CONTACTS_URI, id);
        }
    }

    public int getId() {
        if (isFromPhonebook()) {
            return ContactsManagerImpl.getPhonebookContactId(this);
        } else if (isInvite()) {
            return ContactsManagerImpl.getInviteContactId(this);
        } else {
            return id;
        }
    }

    public boolean shouldHideStatus() {
        return (
                isFromPhonebook() || isSaved() || isRequest() ||
                        isInvite() || directory || isDeclinedRequest
        );
    }

    public boolean mayAddToContact() {
        return !iAm && id == 0 && directory && !blocked && !isFromPhonebook();
    }

    public int getStatus() {
        return contactStatus != null ? contactStatus.getStatusId() : ContactStatus.STATUS_OFFLINE;
    }

    public String getExtraStatus() {
        return contactStatus != null ? contactStatus.getExtraStatus() : "";
    }

    private static List<String> getSipNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactSip);
    }

    private static List<String> getPhoneNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactPhone);
    }

    private static List<String> getNumbers(
            ContactsContactList contactModels, ContactsContactType type) {
        ContactsContactModel contactModel;
        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < contactModels.size(); i++) {
            contactModel = contactModels.get(i);
            if (contactModel.getType() == type) {
                numbers.add(contactModel.getIdentity());
            }
        }
        return numbers;
    }

}
