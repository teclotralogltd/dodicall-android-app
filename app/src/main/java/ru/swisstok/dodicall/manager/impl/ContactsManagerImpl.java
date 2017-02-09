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

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.BusinessLogicCallback;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Logger;
import ru.uls_global.dodicall.BusinessLogic;

import static ru.swisstok.dodicall.provider.DataProvider.FILTER_SUBSCRIPTIONS;

public class ContactsManagerImpl extends BaseManagerImpl implements ContactsManager {

    private static final String TAG = "ContactsManagerImpl";

    private static final int PHONEBOOK_MAGIC_ID = 39837770;
    private static final int INVITE_MAGIC_ID = 98543598;
    private static final String ID_MY = "My";

    private ConcurrentHashMap<Integer, Contact> mContacts;
    private Map<String, Contact> mOtherContacts;
    private Map<String, Contact> mDdcContacts;
    private List<String> mNewlyAddedContacts;
    private volatile Contact mMyContact;
    private Balance mBalance;

    private static volatile ContactsManager sInstance;

    private ContactsManagerImpl() {
        mContacts = new ConcurrentHashMap<>();
        mOtherContacts = new HashMap<>();
        mDdcContacts = new HashMap<>();
        mNewlyAddedContacts = new ArrayList<>();
    }

    public static ContactsManager getInstance() {
        if (sInstance == null) {
            synchronized (ContactsManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new ContactsManagerImpl();
                }
            }
        }
        return sInstance;
    }

    @Override
    public void onCallback(BusinessLogicCallback.Event event, ArrayList<String> ids) {
        switch (event) {
            case Contacts:
                prepareContacts();
                break;
            case ContactsPresence:
                updateStatus(ids);
                break;
            case ContactSubscriptions:
                processSubscription(ids);
                break;
            case LoggedIn:
                mBalance = BL.getBalance();
                break;
        }
    }

    @Override
    public List<Contact> getContacts(int filter, @Nullable String selection, boolean withRequests, boolean withInvites) {
        List<Contact> result = filterContacts(new ArrayList<>(mContacts.values()), filter, selection == null ? "" : selection, withRequests, withInvites);
        if (!mNewlyAddedContacts.isEmpty()) {
            Stream.of(result).
                    filter(contact -> !TextUtils.isEmpty(contact.dodicallId)).
                    forEach(contact -> contact.newlyAcceptedInvite = mNewlyAddedContacts.contains(contact.dodicallId));
        }
        Collections.sort(result, new ContactComparator(BusinessLogic.GetInstance().GetUserSettings().getGuiLanguage()));
        if (filter == FILTER_SUBSCRIPTIONS) {
            Collections.sort(result, subscriptionsComparator);
        }
        return result;
    }

    @Override
    public Contact getContactById(int id) {
        return mContacts.get(id);
    }

    @Override
    public Contact getMyContact() {
        if (mMyContact == null) {
            synchronized (this) {
                if (mMyContact == null) {
                    mMyContact = BL.getMyInfo();
                }
            }
        }
        return mMyContact;
    }

    @Override
    public Balance getBalance() {
        return mBalance;
    }

    @Override
    public void updateBalance(Balance balance) {
        mBalance = balance;
    }

    @Override
    public List<Contact> getNewInvites() {
        List<Contact> result = new ArrayList<>();
        Stream.of(mContacts.values()).filter(contact -> contact.newInvite).forEach(result::add);
        return result;
    }

    @Override
    public Contact saveContact(Contact contact, int currentId) {
        Contact updatedContact = BL.saveContact(contact);
        int newId = updatedContact == null ? currentId : updatedContact.id;
        D.log(TAG, "[insert] newId: %d", newId);
        if (newId > 0) {
            mContacts.remove(currentId);
        }
        mContacts.put(newId, updatedContact);

        notifySubscribers(CONTACTS_UPDATED, null);
        return updatedContact;
    }

    @Override
    public void deleteContact(Contact contact) {
        boolean success = BL.deleteContact(contact);
        if (success) {
            mContacts.remove(contact.getId());
            notifySubscribers(CONTACT_REMOVED, null);
        }
    }

    @Override
    public Contact blockContact(Contact contact, boolean blocked) {
        contact.blocked = blocked;
        BL.saveContact(contact);
        mContacts.put(contact.getId(), contact);
        notifySubscribers(CONTACTS_UPDATED, contact);
        return contact;
    }

    @Override
    public Contact addToWhiteList(Contact contact, boolean white) {
        contact.white = white;
        if (white) {
            contact.blocked = false;
        }
        BL.saveContact(contact);
        mContacts.put(contact.getId(), contact);
        if (!white && filterWhite(new ArrayList<>(mContacts.values())).isEmpty()) {
            BusinessLogic.GetInstance().SaveUserSettings(Preferences.TableColumn.COMMON_WHITE_LIST, false);
        }
        notifySubscribers(CONTACTS_UPDATED, contact);
        return contact;
    }

    @Override
    public Contact declineContact(Contact contact) {
        boolean success = BL.declineRequest(contact);
        if (success) {
            contact.directory = true;
            contact.invite = false;
            contact.subscriptionRequest = false;
            contact.subscriptionState = Contact.SUBSCRIPTION_STATE_NONE;
            mContacts.remove(getInviteContactId(contact));
            notifySubscribers(CONTACTS_UPDATED, contact);
        }
        return contact;
    }

    @Override
    public Contact acceptRequest(Contact contact) {
        Contact updatedContact = BL.saveContact(contact);
        mNewlyAddedContacts.add(updatedContact.dodicallId);
        mContacts.remove(getInviteContactId(contact));
        mContacts.put(updatedContact.getId(), updatedContact);
        notifySubscribers(CONTACTS_ACCEPTED, updatedContact);
        return updatedContact;
    }

    @Override
    public void readInvite(Contact contact) {
        if (BL.readInvite(contact)) {
            contact.newInvite = false;
            notifySubscribers(INVITE_READ, null);
        }
    }

    @Override
    public Contact updateFavorite(Contact contact, String sip) {
        Contact updatedContact = BL.updateFavourite(contact, sip);
        mContacts.put(updatedContact.getId(), updatedContact);
        return updatedContact;
    }

    @Override
    public void addOtherContact(Contact contact) {
        mOtherContacts.put(contact.dodicallId, contact);
    }

    @Override
    public void cleanNewAccepts() {
        mNewlyAddedContacts.clear();
        Stream.of(mContacts.values()).
                filter(contact -> !TextUtils.isEmpty(contact.dodicallId)).
                forEach(contact -> contact.newlyAcceptedInvite = false);
    }

    @Override
    public void clearCache() {
        mContacts.clear();
        mMyContact = null;
        mBalance = null;
    }

    private void prepareContacts() {
        Logger.onOperationStart("GetContacts");
        if (mContacts.isEmpty()) {
            mMyContact = BL.getMyInfo();
            List<Contact> contacts = BL.getContacts();
            for (Contact contact : contacts) {
                if (contact.invite) {
                    mContacts.put(getInviteContactId(contact), contact);
                    continue;
                }
                if (TextUtils.isEmpty(contact.phonebookId) ||
                        !TextUtils.isEmpty(contact.nativeId)) {
                    mContacts.put(contact.id, contact);
                } else {
                    mContacts.put(getPhonebookContactId(contact), contact);
                }
            }
        }
        ArrayList<Contact> updatedContacts = new ArrayList<>();
        ArrayList<Contact> removedContacts = new ArrayList<>();
        BL.getUpdatesForContacts(updatedContacts, removedContacts);
        if (!updatedContacts.isEmpty()) {
            for (Contact updatedContact : updatedContacts) {
                if (!TextUtils.isEmpty(updatedContact.dodicallId)) {
                    notifySubscribers(AVATAR_LOADED, updatedContact);
//                    NotificationsUtils.NotificationType notificationType = NotificationsUtils.checkContactForPendingNotification(updatedContact.dodicallId);
//                    updateNotificationByType(notificationType);
                }
                if (!updatedContact.iAm) {
                    if (updatedContact.invite) {
                        mContacts.put(getInviteContactId(updatedContact), updatedContact);
                        continue;
                    }

                    if (!TextUtils.isEmpty(updatedContact.dodicallId) &&
                            mDdcContacts.containsKey(updatedContact.dodicallId) &&
                            !mDdcContacts.get(updatedContact.dodicallId).avatarPath.equals(updatedContact.avatarPath)) {
                        mDdcContacts.put(updatedContact.dodicallId, updatedContact);
                    } else if (!TextUtils.isEmpty(updatedContact.dodicallId) &&
                            mOtherContacts.containsKey(updatedContact.dodicallId) &&
                            !updatedContact.avatarPath.equals(mOtherContacts.get(updatedContact.dodicallId).avatarPath)) {
                        mOtherContacts.put(updatedContact.dodicallId, updatedContact);
                    } else if (TextUtils.isEmpty(updatedContact.phonebookId) && updatedContact.subscriptionState == Contact.SUBSCRIPTION_STATE_NONE) {
                        mOtherContacts.put(updatedContact.dodicallId, updatedContact);
                    } else if ((TextUtils.isEmpty(updatedContact.phonebookId) || !TextUtils.isEmpty(updatedContact.nativeId))) {
                        mContacts.put(updatedContact.id, updatedContact);
                    } else if (!TextUtils.isEmpty(updatedContact.phonebookId)) {
                        mContacts.put(getPhonebookContactId(updatedContact), updatedContact);
                    } else if (updatedContact.subscriptionState == Contact.SUBSCRIPTION_STATE_NONE) {
                        mOtherContacts.put(updatedContact.dodicallId, updatedContact);
                    }
                }
            }
            if (!removedContacts.isEmpty()) {
                for (Contact removedContact : removedContacts) {
                    if (removedContact.invite) {
                        mContacts.put(getInviteContactId(removedContact), removedContact);
                        continue;
                    }
                    if (TextUtils.isEmpty(removedContact.phonebookId) ||
                            !TextUtils.isEmpty(removedContact.nativeId)) {
                        mContacts.remove(removedContact.id);
                    } else {
                        mContacts.remove(getPhonebookContactId(removedContact));
                    }
                }
            }
            Logger.onOperationEnd("GetContacts");
        }
        notifySubscribers(CONTACTS_UPDATED, null);
    }

    private void updateStatus(ArrayList<String> ids) {
        if (!ids.isEmpty()) {
            Map<String, ContactStatus> statuses = BL.getContactStatus(ids);
            Stream.of(mContacts.values())
                    .filter(contact -> statuses.containsKey(contact.xmppId))
                    .forEach(contact -> {
                        contact.contactStatus = statuses.get(contact.xmppId);
                    });
            if (ids.contains(ID_MY)) {
                getMyContact().contactStatus = statuses.get(ID_MY);
                notifySubscribers(MainReceiver.ACTION_MAIN_STATUS_UPDATE, statuses.get(ID_MY));
            }

            notifySubscribers(ACTION_USERS_STATUSES_UPDATED, ids);
        }
    }

    private void processSubscription(ArrayList<String> ids) {
        Map<String, Contact> contactMap = BL.getSubscriptionStatuses(ids);
        Stream.of(mContacts.values())
                .filter(contact -> contactMap.containsKey(contact.xmppId))
                .forEach(contact -> {
                    Contact updatedContact = contactMap.get(contact.xmppId);
                    D.log(TAG, "[CallbackFunction.run][profile_debug] ContactsSubscription; state: %s; ask: %s",
                            updatedContact.subscriptionState, updatedContact.subscriptionRequest);
                    contact.subscriptionRequest = updatedContact.subscriptionRequest;
                    contact.subscriptionState = updatedContact.subscriptionState;
                    if (contact.id == 0) {
                        contact.invite = updatedContact.invite;
                        contact.newInvite = updatedContact.newInvite;
                    }
                });

        Stream.of(mDdcContacts.values())
                .filter(contact -> contactMap.containsKey(contact.xmppId))
                .forEach(contact -> {
                    Contact updatedContact = contactMap.get(contact.xmppId);
                    D.log(TAG, "[CallbackFunction.run][profile_debug] ContactsSubscription; state: %s; ask: %s",
                            updatedContact.subscriptionState, updatedContact.subscriptionRequest);
                    contact.subscriptionRequest = updatedContact.subscriptionRequest;
                    contact.subscriptionState = updatedContact.subscriptionState;
                });
        notifySubscribers(CONTACTS_UPDATED, ids);
    }

    private static List<Contact> filterContacts(List<Contact> list, int filter, @Nullable String query, boolean withRequests, boolean withInvites) {
        switch (filter) {
            case ToolBarSpinnerAdapter.FILTER_ALL:
                return filterInvitations(filterBlocked(filterText(list, query), false));
            case ToolBarSpinnerAdapter.FILTER_DDC:
                return filterBlocked(filterText(filterDodicall(list, withRequests, withInvites), query), false);
            case ToolBarSpinnerAdapter.FILTER_PHONE:
                return filterInvitations(filterBlocked(filterText(filterPhone(list), query), false));
            case ToolBarSpinnerAdapter.FILTER_SAVED:
                return filterInvitations(filterBlocked(filterText(filterSaved(list), query), false));
            case ToolBarSpinnerAdapter.FILTER_BLOCKED:
                return filterInvitations(filterText(filterBlocked(list, true), query));
            case ToolBarSpinnerAdapter.FILTER_WHITE:
                return filterInvitations(filterBlocked(filterText(filterWhite(list), query), false));
            case FILTER_SUBSCRIPTIONS:
                return filterSubscriptions(list);
        }
        return list;
    }

    private static List<Contact> filterInvitations(List<Contact> list) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterInvitations");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (!contact.invite) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterInvitations", result.size());
        return result;
    }

    private static List<Contact> filterSubscriptions(List<Contact> list) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterSubscriptions");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (contact.invite || contact.subscriptionRequest) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterSubscriptions", result.size());
        return result;
    }

    private static List<Contact> filterText(List<Contact> list, String query) {
        if (TextUtils.isEmpty(query)) {
            return list;
        }
        query = query.toLowerCase().trim();
        if (TextUtils.isEmpty(query)) {
            return list;
        }
        Logger.onOperationStart("FilterText");
        List<Contact> result = new ArrayList<>();
        if (TextUtils.isDigitsOnly(query)) {
            for (Contact contact : list) {
                if (contact == null) {
                    continue;
                }
                for (String sip : contact.sips) {
                    if (sip != null && sip.contains(query)) {
                        result.add(contact);
                        break;
                    }
                }
                for (String phone : contact.phones) {
                    if (phone != null && phone.contains(query)) {
                        result.add(contact);
                        break;
                    }
                }
            }
        } else {
            for (Contact contact : list) {
                if (contact == null) {
                    continue;
                }
                if (contact.firstName.toLowerCase().contains(query) ||
                        contact.lastName.toLowerCase().contains(query)) {
                    result.add(contact);
                }
            }
        }
        Logger.onOperationEnd("FilterText", result.size());
        return result;
    }

    private static List<Contact> filterWhite(List<Contact> list) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterWhite");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (contact.white) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterWhite", result.size());
        return result;
    }

    private static List<Contact> filterBlocked(List<Contact> list, boolean blocked) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterBlocked");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (contact.blocked == blocked) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterBlocked", result.size());
        return result;
    }

    private static List<Contact> filterSaved(List<Contact> list) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterSaved");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (!TextUtils.isEmpty(contact.nativeId)) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterSaved", result.size());
        return result;
    }

    private static List<Contact> filterPhone(List<Contact> list) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterPhone");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (TextUtils.isEmpty(contact.dodicallId) && TextUtils.isEmpty(contact.nativeId)) {
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterPhone", result.size());
        return result;
    }

    private static List<Contact> filterDodicall(List<Contact> list, boolean withRequests, boolean withInvites) {
        List<Contact> result = new ArrayList<>();
        Logger.onOperationStart("FilterDodicall");
        for (Contact contact : list) {
            if (contact == null) {
                continue;
            }
            if (!TextUtils.isEmpty(contact.dodicallId) /*&& contact.id > 0*/) {
                if (!withRequests && contact.subscriptionRequest) {
                    continue;
                }
                if (!withInvites && contact.invite) {
                    continue;
                }
                result.add(contact);
            }
        }
        Logger.onOperationEnd("FilterDodicall", result.size());
        return result;
    }

    public static int getInviteContactId(Contact contact) {
        return Math.abs(INVITE_MAGIC_ID + contact.dodicallId.hashCode());
    }

    public static int getPhonebookContactId(Contact contact) {
        return PHONEBOOK_MAGIC_ID + Integer.valueOf(contact.phonebookId);
    }

    private static class ContactComparator implements Comparator<Contact> {

        private Collator mCollator;

        private ContactComparator(String locale) {
            mCollator = Collator.getInstance(new Locale(locale));
            mCollator.setStrength(Collator.PRIMARY);
        }

        @Override
        public int compare(Contact lhs, Contact rhs) {
            return compare(
                    TextUtils.isEmpty(lhs.firstName) ? " " : lhs.firstName,
                    TextUtils.isEmpty(rhs.firstName) ? " " : rhs.firstName
            );
        }

        private int compare(String lfn, String rfn) {
            if (!Character.isLetter(lfn.charAt(0)) && Character.isLetter(rfn.charAt(0))) {
                return 1;
            } else if (!Character.isLetter(lfn.charAt(0)) && !Character.isLetter(rfn.charAt(0))) {
                return mCollator.compare(lfn, rfn);
            } else if (Character.isLetter(lfn.charAt(0)) && !Character.isLetter(rfn.charAt(0))) {
                return -1;
            } else {
                return mCollator.compare(lfn, rfn);
            }
        }

    }

    private static final Comparator<Contact> subscriptionsComparator = (lhs, rhs) -> (lhs.subscriptionRequest ? 1 : -1);
}
