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

package ru.swisstok.dodicall.bl;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.swisstok.dodicall.DodicallApplication;
import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.CallHistoryDetail;
import ru.swisstok.dodicall.api.CallStatistics;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Logger;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BalanceResult;
import ru.uls_global.dodicall.BaseResult;
import ru.uls_global.dodicall.BaseUserStatus;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CallAddressType;
import ru.uls_global.dodicall.CallHistoryEntryList;
import ru.uls_global.dodicall.CallHistoryEntryModel;
import ru.uls_global.dodicall.CallHistoryModel;
import ru.uls_global.dodicall.CallHistoryPeerList;
import ru.uls_global.dodicall.CallHistoryPeerModel;
import ru.uls_global.dodicall.CallModel;
import ru.uls_global.dodicall.CallModelList;
import ru.uls_global.dodicall.CallStatisticsModel;
import ru.uls_global.dodicall.ChatMessageModel;
import ru.uls_global.dodicall.ChatMessageModelList;
import ru.uls_global.dodicall.ChatMessageType;
import ru.uls_global.dodicall.ChatModel;
import ru.uls_global.dodicall.ChatModelList;
import ru.uls_global.dodicall.ContactModel;
import ru.uls_global.dodicall.ContactModelList;
import ru.uls_global.dodicall.ContactPresenceStatusList;
import ru.uls_global.dodicall.ContactPresenceStatusModel;
import ru.uls_global.dodicall.ContactSubscriptionMap;
import ru.uls_global.dodicall.ContactSubscriptionModel;
import ru.uls_global.dodicall.ContactSubscriptionState;
import ru.uls_global.dodicall.ContactSubscriptionStatus;
import ru.uls_global.dodicall.ContactsContactList;
import ru.uls_global.dodicall.ContactsContactModel;
import ru.uls_global.dodicall.ContactsContactSet;
import ru.uls_global.dodicall.ContactsContactType;
import ru.uls_global.dodicall.HistoryFilterModel;
import ru.uls_global.dodicall.HistoryFilterSelector;
import ru.uls_global.dodicall.OptionalContactModel;
import ru.uls_global.dodicall.ResultErrorCode;
import ru.uls_global.dodicall.StringList;
import ru.uls_global.dodicall.StringSet;

public class BL {

    public static final String FAVORITE_MARKER = "FAVORITE:";
    public static final int CHAT_MESSAGES_PAGE_SIZE = 20;

    public static ArrayList<Contact> getContacts() {
        Logger.onOperationStart("GetContactsBL");
        ContactModelList contactModels = BusinessLogic.GetInstance().GetAllContacts();
        Logger.onOperationEnd("GetContactsBL");
        return newContacts(contactModels);
    }

    @WorkerThread
    public static Contact getMyInfo() {
        ContactModel myInfo = BusinessLogic.GetInstance().GetAccountData();
        return newContact(myInfo);
    }

    public static Balance getBalance() {
        BalanceResult balanceResult = BusinessLogic.GetInstance().GetBalance();
        if (balanceResult.getHasBalance()) {
            Balance balance = new Balance();
            if (balanceResult.getSuccess()) {
                balance.currency = balanceResult.getBalanceCurrency().swigValue();
                balance.value = balanceResult.getBalanceValue();
            }
            return balance;
        }
        return null;
    }

    @WorkerThread
    public static Contact retrieveContactByNumber(String number) {
        ContactModel contact = BusinessLogic.GetInstance().RetriveContactByNumber(number);
        return newContact(contact);
    }

    public static ArrayList<Contact> searchContacts(String query) {
        ContactModelList searchList = new ContactModelList();
        BusinessLogic.GetInstance().FindContactsInDirectory(searchList, query == null ? "" : query);

        ArrayList<Contact> contacts = newContacts(searchList);
        loadAvatars(contacts);
        return contacts;
    }

    public static void loadAvatars(List<Contact> contacts) {
        StringSet ids = new StringSet();
        for (Contact contact : contacts) {
            if (!TextUtils.isEmpty(contact.dodicallId) && TextUtils.isEmpty(contact.avatarPath)) {
                ids.insert(contact.dodicallId);
            }
        }
        if (!ids.empty()) {
            BusinessLogic.GetInstance().DownloadAvatarForContactsWithDodicallIds(ids);
        }
    }

    public static void getUpdatesForContacts(ArrayList<Contact> updatedContacts, ArrayList<Contact> removedContacts) {
        ContactModelList updated = new ContactModelList();
        ContactModelList deleted = new ContactModelList();
        BusinessLogic.GetInstance().RetrieveChangedContacts(updated, deleted);
        updatedContacts.addAll(newContacts(updated));
        removedContacts.addAll(newContacts(deleted));
    }

    public static Contact saveContact(Contact contact) {
        ContactModel updatedContact = BusinessLogic.GetInstance().SaveContact(newContact(contact));
        return newContact(updatedContact);
    }

    public static boolean deleteContact(Contact contact) {
        return BusinessLogic.GetInstance().DeleteContact(newContact(contact));
    }

    public static boolean readInvite(Contact contact) {
        contact.subscriptionState = ContactSubscriptionStatus.ContactSubscriptionStatusReaded.swigValue();
        return BusinessLogic.GetInstance().MarkSubscriptionAsOld(contact.xmppId);
    }

    public static boolean declineRequest(Contact contact) {
        return BusinessLogic.GetInstance().AnswerSubscriptionRequest(newContact(contact), false);
    }

    public static Contact updateFavourite(Contact contact, String sip) {
        ContactModel contactModel = newContact(contact);
        ContactsContactList contactModels = BusinessLogic.GetInstance().GetContacts(contactModel);
        String sipFavorite = sip.replace(FAVORITE_MARKER, "");
        ContactsContactSet newContacts = new ContactsContactSet();
        for (int i = 0; i < contactModels.size(); i++) {
            contactModels.get(i).setFavourite(
                    contactModels.get(i).getIdentity().equals(sipFavorite)
            );
            newContacts.insert(contactModels.get(i));
        }
        contactModel.setContacts(newContacts);
        ContactModel updatedModel = BusinessLogic.GetInstance().SaveContact(contactModel);
        return newContact(updatedModel);
    }

    public static Map<String, ContactStatus> getContactStatus(List<String> ids) {
        Logger.onOperationStart("ContactStatusUpdatedBLGetByIds");
        ContactPresenceStatusList statuses = BusinessLogic.GetInstance().GetPresenceStatusesByXmppIds(newStringList(ids));
        Logger.onOperationEnd("ContactStatusUpdatedBLGetByIds", (int) statuses.size());
        Map<String, ContactStatus> result = new HashMap<>();
        for (int i = 0; i < statuses.size(); i++) {
            ContactStatus contactStatus = newContactStatus(statuses.get(i));
            result.put(contactStatus.getXmppId(), contactStatus);
        }
        return result;
    }

    public static ContactStatus getContactStatus(String id) {
        Logger.onOperationStart("ContactStatusUpdatedBLGetById");
        ContactPresenceStatusModel status = BusinessLogic.GetInstance().GetPresenceStatusByXmppId(id);
        Logger.onOperationEnd("ContactStatusUpdatedBLGetById");
        return newContactStatus(status);
    }

    public static Map<String, Contact> getSubscriptionStatuses(List<String> ids) {
        final ContactSubscriptionMap subscriptions = BusinessLogic.GetInstance().GetSubscriptionStatusesByXmppIds(newStringList(ids));
        Map<String, Contact> result = new HashMap<>();
        for (String id : ids) {
            ContactSubscriptionModel subscriptionModel = subscriptions.get(id);

            Contact contact = new Contact();
            contact.xmppId = id;
            contact.subscriptionRequest = subscriptionModel.getAskForSubscription();
            contact.subscriptionState = subscriptionModel.getSubscriptionState().swigValue();
            contact.invite = subscriptionModel.getSubscriptionState() == ContactSubscriptionState.ContactSubscriptionStateTo;
            contact.newInvite = contact.invite && subscriptionModel.getSubscriptionStatus() == ContactSubscriptionStatus.ContactSubscriptionStatusNew;
            result.put(id, contact);
        }
        return result;
    }

    @WorkerThread
    public static void saveStatus(BaseUserStatus status, String statusExtra) {
        BusinessLogic.GetInstance().SaveUserSettings("UserBaseStatus", status);
        BusinessLogic.GetInstance().SaveUserSettings("UserExtendedStatus", statusExtra);
    }

    @WorkerThread
    public static void sendReadyForCall(String sipNumber) {
        BusinessLogic.GetInstance().SendReadyForCallAfterStart(sipNumber);
    }

    @WorkerThread
    public static int getAllMissedCalls() {
        int missed = BusinessLogic.GetInstance().GetNumberOfMissedCalls();
        return missed;
    }

    @WorkerThread
    public static void setAllCallHistoryRead() {
        CallHistoriesList callHistories = BL.getCallHistoriesList();
        if (callHistories != null && !callHistories.isEmpty()) {
            ArrayList<String> peers = new ArrayList<>();
            for (CallHistory callHistory : callHistories) {
                if (callHistory.statistics.numberOfMissedCalls > 0) {
                    peers.add(callHistory.id);
                }
            }

            if (!peers.isEmpty()) {
                setCallHistoryRead(peers.toArray(new String[peers.size()]));
            }
        }
    }

    @WorkerThread
    public static void setCallHistoryRead(String... ids) {
        StringList peers = new StringList();
        for (String id : ids) {
            peers.add(id);
        }
        if (!peers.isEmpty()) {
            HistoryFilterModel hfm = new HistoryFilterModel(HistoryFilterSelector.HistoryFilterAny);
            hfm.setPeers(peers);

            if (BusinessLogic.GetInstance().SetCallHistoryReaded(hfm)) {
                LocalBroadcast.sendBroadcast(DodicallApplication.getContext(), DataProvider.ACTION_HISTORY_UPDATED);
            }
        }
    }

    @WorkerThread
    public static CallHistoriesList getCallHistoriesList(@Nullable String... ids) {
        Logger.onOperationStart("LoadCallHistory");
        HistoryFilterModel hfm = new HistoryFilterModel(HistoryFilterSelector.HistoryFilterAny);

        if (ids != null && ids.length > 0) {
            hfm.setPeers(newStringList(Arrays.asList(ids)));
        }

        Logger.onOperationStart("LoadCallHistoryBL");
        CallHistoryModel chm = BusinessLogic.GetInstance().GetCallHistory(hfm, false);
        Logger.onOperationEnd("LoadCallHistoryBL");

        Logger.onOperationStart("LoadCallHistoryCoping");
        CallHistoryPeerList callHistoryPeerList = chm.getPeers();
        CallHistoriesList result = new CallHistoriesList((int) callHistoryPeerList.size());

        for (int i = 0, size = (int) callHistoryPeerList.size(); i < size; ++i) {
            CallHistory callHistory = newCallHistory(callHistoryPeerList.get(i));
            result.add(callHistory);
        }
        Logger.onOperationEnd("LoadCallHistoryCoping");
        Logger.onOperationEnd("LoadCallHistory", result.size());
        return result;
    }

    @WorkerThread
    public static CallHistoryDetailsList getCallHistoryDetailsList(CallHistory ch) {
        Logger.onOperationStart("LoadCallHistoryDetails");
        HistoryFilterModel hfm = new HistoryFilterModel(HistoryFilterSelector.HistoryFilterAny);
        hfm.setPeers(newStringList(Collections.singletonList(ch.id)));

        Logger.onOperationStart("LoadCallHistoryDetailsBL");
        CallHistoryModel chm = BusinessLogic.GetInstance().GetCallHistory(hfm, true);
        Logger.onOperationEnd("LoadCallHistoryDetailsBL");

        if (chm.getPeers().size() == 0) {
            return new CallHistoryDetailsList(0);
        }

        Logger.onOperationStart("LoadCallHistoryDetailsCoping");
        CallHistoryEntryList chel = chm.getPeers().get(0).getDetailsList();

        int size = (int) chel.size();
        CallHistoryDetailsList result = new CallHistoryDetailsList(size);

        for (int i = 0; i < size; ++i) {
            result.add(newCallHistoryDetail(chel.get(i)));
        }
        Logger.onOperationEnd("LoadCallHistoryDetailsCoping");
        Logger.onOperationEnd("LoadCallHistoryDetails");

        return result;
    }

    @WorkerThread
    public static CallHistory newCallHistory(CallHistoryPeerModel chpm) {
        CallHistory result = new CallHistory();
        result.id = chpm.GetId();
        result.addressType = chpm.getAddressType().swigValue();
        result.identity = chpm.getIdentity();

        if (chpm.getContact().isPresent()) {
            result.contact = newContact(chpm.getContact().get());
        }

        result.statistics = newCallStatistics(chpm.getStatistics());
        result.detail = newCallHistoryDetail(chpm.getDetailsList().get(0));

        return result;
    }

    @WorkerThread
    public static CallHistoryPeerModel newCallHistoryPeerModel(CallHistory ch) {
        CallHistoryPeerModel result = new CallHistoryPeerModel();

        result.setAddressType(CallAddressType.swigToEnum(ch.addressType));
        result.setIdentity(ch.identity);

        if (ch.contact != null) {
            result.setContact(new OptionalContactModel(BusinessLogic.GetInstance().GetContactByIdFromCache(ch.contact.id)));
        }

        return result;
    }

    @WorkerThread
    public static boolean deleteMessages(List<ChatMessage> messagesToDelete) {
        StringList idsList = new StringList();
        for (ChatMessage chatMessage : messagesToDelete) {
            idsList.add(chatMessage.getId());
        }
        return BusinessLogic.GetInstance().DeleteMessages(idsList);
    }

    private static CallHistoryDetail newCallHistoryDetail(CallHistoryEntryModel chem) {
        CallHistoryDetail result = new CallHistoryDetail();

        result.direction = chem.getDirection().swigValue();
        result.encryption = chem.getEncription().swigValue();
        result.durationInSeconds = chem.getDurationSec();
        result.endMode = chem.getEndMode().swigValue();
        result.startTime = chem.getStartTime();
        result.historyStatus = chem.getHistoryStatus().swigValue();
        result.addressType = chem.getAddressType().swigValue();
        result.historySource = chem.getHistorySource().swigValue();
        result.id = chem.getId();

        return result;
    }

    @WorkerThread
    public static CallStatistics newCallStatistics(CallStatisticsModel csm) {
        CallStatistics result = new CallStatistics();

        result.numberOfIncomingSuccessfulCalls = csm.getNumberOfIncomingSuccessfulCalls();
        result.numberOfOutgoingSuccessfulCalls = csm.getNumberOfOutgoingSuccessfulCalls();
        result.numberOfIncomingUnsuccessfulCalls = csm.getNumberOfIncomingUnsuccessfulCalls();
        result.numberOfOutgoingUnsuccessfulCalls = csm.getNumberOfOutgoingUnsuccessfulCalls();
        result.numberOfMissedCalls = csm.getNumberOfMissedCalls();
        result.hasOutgoingEncryptedCall = csm.getHasOutgoingEncryptedCall();
        result.hasIncomingEncryptedCall = csm.getHasIncomingEncryptedCall();
        result.wasConference = csm.getWasConference();

        return result;
    }

    @WorkerThread
    public static ArrayList<Contact> newContacts(ContactModelList contactModels) {
        ArrayList<Contact> contacts = new ArrayList<>();
        for (int i = 0; i < contactModels.size(); i++) {
            ContactModel contactModel = contactModels.get(i);
            contacts.add(newContact(contactModel));
        }
        return contacts;
    }

    @WorkerThread
    public static Contact newContact(ContactModel cm) {
        Contact result = new Contact();

        result.id = cm.getId();
        result.dodicallId = cm.getDodicallId();
        result.phonebookId = cm.getPhonebookId();
        result.nativeId = cm.getNativeId();
        result.xmppId = cm.GetXmppId();
        result.firstName = cm.getFirstName();
        result.lastName = cm.getLastName();
        result.middleName = cm.getMiddleName();
        result.avatarPath = cm.getAvatarPath();
        result.blocked = cm.getBlocked();
        result.white = cm.getWhite();

        ContactPresenceStatusModel cpsm = BusinessLogic.GetInstance().GetPresenceStatusByXmppId(result.xmppId);
        result.contactStatus = newContactStatus(cpsm);

        result.invite = DataProvider.isInvite(cm);
        result.subscriptionRequest = cm.getSubscription().getAskForSubscription();
        result.subscriptionState = cm.getSubscription().getSubscriptionState().swigValue();
        result.newInvite = DataProvider.isNewInvite(cm);
        result.directory = DataProvider.isDirectory(cm);
        result.iAm = cm.getIam();
        result.isMine = isMineContact(cm);
        result.isDeclinedRequest = DataProvider.isDeclinedRequest(cm);

        ContactsContactList contacts = BusinessLogic.GetInstance().GetContacts(cm);

        result.phones = getPhoneNumbers(contacts);
        result.sips = getSipNumbers(contacts);

        return result;
    }

    @WorkerThread
    public static ContactModel newContact(Contact contact) {
        ContactModel contactModel = new ContactModel();
        contactModel.setFirstName(contact.firstName);
        contactModel.setLastName(contact.lastName);
        contactModel.setMiddleName(!TextUtils.isEmpty(contact.middleName) ? contact.middleName : "");

        contactModel.setPhonebookId(!TextUtils.isEmpty(contact.phonebookId) ? contact.phonebookId : "");
        contactModel.setDodicallId(!TextUtils.isEmpty(contact.dodicallId) ? contact.dodicallId : "");
        contactModel.setNativeId(!TextUtils.isEmpty(contact.nativeId) ? contact.nativeId : "");
        contactModel.setId(contact.id);

        contactModel.setWhite(contact.white);
        contactModel.setBlocked(contact.blocked);

        contactModel.setSubscription(new ContactSubscriptionModel(ContactSubscriptionState.swigToEnum(contact.subscriptionState), contact.subscriptionRequest));

        ContactsContactSet phonesSet = new ContactsContactSet();
        ContactsContactModel phoneModel;
        if (!TextUtils.isEmpty(contact.xmppId)) {
            phonesSet.insert(new ContactsContactModel(contact.xmppId, ContactsContactType.ContactsContactXmpp));
        }
        if (CollectionUtils.isNotEmpty(contact.phones)) {
            for (String phone : contact.phones) {
                phoneModel = new ContactsContactModel();
                phoneModel.setIdentity(phone);
                phoneModel.setType(ContactsContactType.ContactsContactPhone);
                phonesSet.insert(phoneModel);
            }
        }
        if (CollectionUtils.isNotEmpty(contact.sips)) {
            for (String phone : contact.sips) {
                phoneModel = new ContactsContactModel();
                phoneModel.setIdentity(phone);
                phoneModel.setType(ContactsContactType.ContactsContactSip);
                phonesSet.insert(phoneModel);
            }
        }
        contactModel.setContacts(phonesSet);

        return contactModel;
    }

    public static ContactStatus newContactStatus(ContactPresenceStatusModel cpsm) {
        return new ContactStatus(cpsm.getXmppId(), cpsm.getBaseStatus().swigValue(), cpsm.getExtStatus());
    }

    private static List<String> getSipNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactSip);
    }

    private static List<String> getPhoneNumbers(ContactsContactList contactModels) {
        return getNumbers(contactModels, ContactsContactType.ContactsContactPhone);
    }

    public static boolean isMineContact(ContactModel contactModel) {
        return (!TextUtils.isEmpty(contactModel.getPhonebookId()) || !TextUtils.isEmpty(contactModel.getNativeId()) ||
                contactModel.getId() > 0) && !(DataProvider.isInvite(contactModel) || contactModel.getSubscription().getAskForSubscription());
    }

    private static List<String> getNumbers(ContactsContactList contactModels, ContactsContactType type) {
        ContactsContactModel contactModel;
        List<String> numbers = new ArrayList<>();

        for (int i = 0, size = (int) contactModels.size(); i < size; i++) {
            contactModel = contactModels.get(i);
            if (contactModel.getType() == type) {
                String number = contactModel.getIdentity();
                if (contactModel.getFavourite() && contactModel.getType() == ContactsContactType.ContactsContactSip) {
                    number = FAVORITE_MARKER + number;
                }
                numbers.add(number);
            }
        }

        return numbers;
    }

    public static StringSet newStringSet(String... values) {
        StringSet stringSet = new StringSet();
        for (String v : values) {
            stringSet.insert(v);
        }

        return stringSet;
    }

    public static StringList newStringList(List<String> values) {
        StringList result = new StringList();
        for (String v : values) {
            result.add(v);
        }

        return result;
    }

    @Nullable
    public static String[] stringListToArray(@Nullable StringList stringList) {
        if (stringList == null) {
            return null;
        }

        int size = (int) stringList.size();
        String[] result = new String[size];

        for (int i = 0; i < size; ++i) {
            result[i] = stringList.get(i);
        }

        return result;
    }

    private BL() {
    }

    public static class ResultCode {
        public final static int ERROR_NO = ResultErrorCode.ResultErrorNo.swigValue();
        public final static int ERROR_SYSTEM = ResultErrorCode.ResultErrorSystem.swigValue();
        public final static int ERROR_SETUP_NOT_COMPLETED = ResultErrorCode.ResultErrorSetupNotCompleted.swigValue();
        public final static int ERROR_AUTH_FAILED = ResultErrorCode.ResultErrorAuthFailed.swigValue();
        public final static int ERROR_NO_NETWORK = ResultErrorCode.ResultErrorNoNetwork.swigValue();

        private ResultCode() {
        }
    }

    public static class LoginResult {
        private boolean success;
        private int resultCode;

        public LoginResult(boolean success, int resultCode) {
            this.success = success;
            this.resultCode = resultCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getResultCode() {
            return resultCode;
        }
    }


    public static LoginResult login(Context context, String login, char[] password, char[] userKey, int area) {
        final BusinessLogic logic = BusinessLogic.GetInstance();
        int networkClass = MainReceiver.getNetworkClass(context);
        logic.SetNetworkTechnology(MainReceiver.getNetworkTechnology(networkClass));
        BaseResult result = logic.Login(login, String.valueOf(password), String.valueOf(userKey), area);
        return new LoginResult(result.getSuccess(), result.getErrorCode().swigValue());
    }

    @MainThread
    public static boolean tryAutoLogin(char[] password, char[] userKey) {
        return BusinessLogic.GetInstance().TryAutoLoginWithPassword(String.valueOf(password), String.valueOf(userKey));
    }

    public static void logout(Context context, ContentResolver contentResolver) throws Exception {
        contentResolver.delete(DataProvider.makeDropDataUri(), null, null);
        BusinessLogic.GetInstance().ClearSavedPassword();
        BusinessLogic.GetInstance().Logout();
        StorageUtils.storePassword(context, new char[]{});
    }

    public static boolean isLoggedIn() {
        return BusinessLogic.GetInstance().IsLoggedIn();
    }

    @WorkerThread
    public static void MarkAllMessagesAsRead() {
        if (!BusinessLogic.GetInstance().MarkAllMessagesAsReaded()) {
            D.log("BL", "MarkAllMessagesAsRead() returned false");
        }
    }

    public static ArrayList<Chat> getChats(String... ids) {
        final ChatModelList chatModelList = new ChatModelList();

        if (ids.length == 0) {
            Logger.onOperationStart("LoadChatsBLAll");
            BusinessLogic.GetInstance().GetAllChats(chatModelList);
            Logger.onOperationEnd("LoadChatsBLAll");
        } else {
            StringSet idSet = new StringSet();
            for (String id : ids) {
                idSet.insert(id);
            }
            Logger.onOperationStart("LoadChatsBLByIds");
            BusinessLogic.GetInstance().GetChatsByIds(idSet, chatModelList);
            Logger.onOperationEnd("LoadChatsBLByIds");
        }
        ArrayList<Chat> result = new ArrayList<>();
        for (int i = 0; i < chatModelList.size(); ++i) {
            final ChatModel chatModel = chatModelList.get(i);
            result.add(convertChatModel(chatModel));
        }
        return result;
    }

    public static Chat getChatById(String chatId) {
        ChatModelList l = new ChatModelList();
        Logger.onOperationStart("LoadChatByIdBL");
        if (BusinessLogic.GetInstance().GetChatsByIds(newStringSet(chatId), l)) {
            Logger.onOperationStart("LoadChatByIdBL");
            if (!l.isEmpty()) {
                return convertChatModel(l.get(0));
            }
        }
        return null;
    }

    public static Chat createChatWithContacts(String[] ids) {
        ContactModelList list = new ContactModelList();
        for (String id : ids) {
            ContactModel cm = BusinessLogic.GetInstance().GetContactByIdFromCache(Integer.valueOf(id));
            list.add(cm);
        }

        ChatModel chatModel = new ChatModel();
        if (BusinessLogic.GetInstance().CreateChatWithContacts(list, chatModel)) {
            return convertChatModel(new ChatModel(chatModel));
        }
        return null;
    }

    public static ArrayList<ChatMessage> getChatMessagesPaged(String chatId, String lastMessageId) {
        final ChatMessageModelList messages = new ChatMessageModelList();

        Logger.onOperationStart("LoadChatMessagesBLPaged");
        BusinessLogic.GetInstance().GetChatMessagesPaged(chatId, CHAT_MESSAGES_PAGE_SIZE, lastMessageId, messages);
        Logger.onOperationEnd("LoadChatMessagesBLPaged");
        ArrayList<ChatMessage> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); ++i) {
            final ChatMessageModel cmm = messages.get(i);
            if (TextUtils.equals(chatId, cmm.getChatId())) {
                result.add(convertChatMessageModel(chatId, cmm));
            }
        }
        return result;
    }

    public static ArrayList<ChatMessage> getChatMessages(String chatId, String[] messageIds) {
        final ChatMessageModelList messages = new ChatMessageModelList();


        if (messageIds == null || messageIds.length == 0) {
            Logger.onOperationStart("LoadChatMessagesBLAll");
            BusinessLogic.GetInstance().GetChatMessages(chatId, messages);
            Logger.onOperationEnd("LoadChatMessagesBLAll");
        } else {
            StringSet ids = new StringSet();
            for (String id : messageIds) {
                ids.insert(id);
            }
            Logger.onOperationStart("LoadChatMessagesBLIds");
            BusinessLogic.GetInstance().GetMessagesByIds(ids, messages);
            Logger.onOperationEnd("LoadChatMessagesBLIds");
        }

        ArrayList<ChatMessage> result = new ArrayList<>();
        for (int i = 0; i < messages.size(); ++i) {
            final ChatMessageModel cmm = messages.get(i);
            if (TextUtils.equals(chatId, cmm.getChatId())) {
                result.add(convertChatMessageModel(chatId, cmm));
            }
        }
        return result;
    }

    public static boolean readChatMessage(String messageId) {
        return BusinessLogic.GetInstance().MarkMessagesAsReaded(messageId);
    }

    public static String sendChatMessage(ChatMessage chatMessage, ChatMessage.MessageAction messageAction) {
        if (chatMessage.isTextMessage()) {
            if (chatMessage.getQuotedMessages() == null || chatMessage.getQuotedMessages().isEmpty()) {
                return BusinessLogic.GetInstance().SendTextMessage(chatMessage.getId(), chatMessage.getChatId(), chatMessage.getContent());
            } else {
                if (replyToMessage(chatMessage, messageAction)) {
                    return "";
                }
            }
        } else if (chatMessage.isContactMessage()) {
            if (BusinessLogic.GetInstance().SendContactToChat(chatMessage.getId(), chatMessage.getChatId(), chatMessage.getSharedContact().id)) {
                return "";
            }
        }
        return "";
    }

    public static boolean replyToMessage(ChatMessage chatMessage, ChatMessage.MessageAction messageAction) {
        StringSet ids = new StringSet();
        Stream.of(chatMessage.getQuotedMessages()).forEach(message -> ids.insert(message.getId()));
        ChatMessageModelList messageModelList = new ChatMessageModelList();
        BusinessLogic.GetInstance().GetMessagesByIds(ids, messageModelList);
        if (messageAction == ChatMessage.MessageAction.Reply) { //hack for reply to quote message with empty body. If content is empty that messageAction -> Quote, to add nesting
            ChatMessage quotedMessage = chatMessage.getQuotedMessages().get(0);
            if (TextUtils.isEmpty(quotedMessage.getContent())) {
                messageAction = ChatMessage.MessageAction.Quote;
            }
        }
        return BusinessLogic.GetInstance().QuoteAndSendMessages(messageModelList, chatMessage.getId(), chatMessage.getChatId(), chatMessage.getContent(), messageAction == ChatMessage.MessageAction.Reply);
    }

    public static List<Call> getCalls() {
        List<Call> result = new ArrayList<>();
        final CallModelList calls = BusinessLogic.GetInstance().GetAllCalls().getSingleCallsList();
        if (!calls.isEmpty()) {
            for (int i = 0; i < calls.size(); i++) {
                CallModel callModel = calls.get(i);
                result.add(new Call(callModel));
            }
        }
        return result;
    }

    public static boolean hasActiveCall() {
        final CallModelList calls = BusinessLogic.GetInstance().GetAllCalls().getSingleCallsList();
        return !calls.isEmpty();
    }

    private static Chat convertChatModel(ChatModel chatModel) {
        return new Chat(chatModel.getId(),
                chatModel.getTitle(),
                chatModel.getActive(),
                chatModel.getTotalMessagesCount(),
                chatModel.getNewMessagesCount(),
                chatModel.getLastModifiedDate().getMillis(),
                chatModel.getIsP2p(),
                convertChatMessageModel(chatModel.getId(), chatModel.getLastMessage().isPresent() ? chatModel.getLastMessage().get() : null),
                convertContacts(chatModel.getContactsList()),
                null);
    }

    private static ChatMessage convertChatMessageModel(String chatId, ChatMessageModel cmm) {
        if (cmm == null) {
            return null;
        }
        ChatNotificationData chatNotificationData = null;
        if (cmm.getType().equals(ChatMessageType.ChatMessageTypeNotification)) {
            if (cmm.getNotificationData() != null && cmm.getNotificationData().isPresent()) {
                ru.uls_global.dodicall.ChatNotificationData cnd = cmm.getNotificationData().get();
                chatNotificationData = new ChatNotificationData(cnd.getType().toString(), convertContacts(cnd.getContactsList()));
            }
        }
        Contact contact = null;
        if (cmm.getType().equals(ChatMessageType.ChatMessageTypeContact)) {
            if (cmm.getContactData() != null && cmm.getContactData().isPresent()) {
                contact = newContact(cmm.getContactData().get());
            }
        }
        ChatMessage cm = new ChatMessage(cmm.getId(),
                chatId,
                cmm.getServered(),
                cmm.getSendTime().getMillis(),
                cmm.getReaded(),
                cmm.getChanged(),
                contact == null ? cmm.getStringContent() : Utils.formatAccountFullName(contact), //TODO: temporary fix for reply
                cmm.getType().toString(),
                newContact(cmm.getSender()),
                chatNotificationData,
                cmm.getRownum(),
                cmm.getEncrypted(),
                contact);
        if (cmm.getQuotedMessages() != null && !cmm.getQuotedMessages().isEmpty()) {
            ChatMessageModelList messages = cmm.getQuotedMessages();
            List<ChatMessage> quotedMessages = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                quotedMessages.add(convertChatMessageModel(messages.get(i).getChatId(), messages.get(i)));
            }
            cm.setQuotedMessages(quotedMessages);
        }
        return cm;
    }

    private static ArrayList<Contact> convertContacts(ContactModelList contactModelList) {
        ArrayList<Contact> contacts = new ArrayList<>();
        for (int k = 0; k < contactModelList.size(); ++k) {
            ContactModel contactModel = contactModelList.get(k);
            contacts.add(newContact(contactModel));
        }
        return contacts;
    }

    public static String getVersion() {
        return BusinessLogic.GetInstance().GetVersion();
    }
}
