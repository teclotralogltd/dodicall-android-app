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

package ru.swisstok.dodicall.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.parceler.Parcels;

import java.lang.ref.WeakReference;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ChangeStatusActivity;
import ru.swisstok.dodicall.activity.ChatActivity;
import ru.swisstok.dodicall.activity.CreateProfileActivity;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.ContactActionAsyncTask;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.ProfileAccountItem;
import ru.swisstok.dodicall.view.ProfileExtraContactItem;
import ru.swisstok.dodicall.view.RoundedImageView;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.GlobalApplicationSettingsModel;
import ru.uls_global.dodicall.ServerAreaModelWrapper;
import ru.uls_global.dodicall.ServerAreasList;

public class ProfileFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Contact>, View.OnClickListener, CreateChatAsyncTask.OnCreateChatListener, ContactActionAsyncTask.ContactActionListener {

    public static final String IS_PERSONAL = "is_personal";
    //    public static final String DDC_SEARCH = "ddc_search";
    public static final String CONTACT = "contact";
    public static final String TAG = "ProfileFragment";

    private ContactsManager mContactsManager;
    private Contact mContact;
    private boolean mIsPersonalProfile = false;

    private LinearLayout mContent;
    private LinearLayout mAccountsList;
    /*
    don't delete
    private LinearLayout mDevicesList;
    */
    private LinearLayout mExtraContactsListWrapper;
    private LinearLayout mExtraContactsList;
    /*
    don't delete
    private LinearLayout mExternalAppWrapper;
    */
    private LinearLayout mBlockedWrapper;
    private LinearLayout mBalanceWrapper;
    private LinearLayout mInviteMsg;
    private RelativeLayout mProfileButtonsWrapper;
    private TextView mEmpty;
    private TextView mName;
    private TextView mContactStatus;
    private TextView mPersonalStatus;
    private TextView mBalanceText;
    private TextView mRequestSentMsg;
    private ImageButton mCallButton;
    private ImageButton mVideoCallButton;
    private ImageButton mChatButton;
    private RoundedImageView mAvatarImage;
    private ImageView mIsDodicallIcon;
    private Button mUnblockButton;
    private ProgressBar mProgress;
    private LinearLayout mAddToContactWrapper;

    private static final class QueryHelper extends AsyncQueryHandler {

        private WeakReference<AsyncQueryListener> mListener;

        private interface AsyncQueryListener {
            void onComplete(int token, Object cookie, int result, Uri uri);
        }

        private QueryHelper(ContentResolver cr, AsyncQueryListener listener) {
            super(cr);
            mListener = new WeakReference<>(listener);
        }

        private QueryHelper(ContentResolver cr) {
            this(cr, null);
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            final AsyncQueryListener listener = mListener.get();
            if (listener != null) {
                listener.onComplete(token, cookie, result, null);
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            final AsyncQueryListener listener = mListener.get();
            if (listener != null) {
                listener.onComplete(token, cookie, result, null);
            }
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            final AsyncQueryListener listener = mListener.get();
            if (listener != null) {
                listener.onComplete(token, cookie, 0, uri);
            }
        }

        public static void deleteProfile(Activity activity, Contact contact) {
            Utils.showConfirm(activity, R.string.delete_user_confirm, (dialog, which) -> {
                //TODO: check it
                final ProgressDialog progress = Utils.showProgress(
                        activity, R.string.profile_delete_progress_msg
                );
                if (DialogInterface.BUTTON_POSITIVE == which) {
                    ContactsManagerImpl.getInstance().deleteContact(contact);
                    if (progress != null) {
                        progress.dismiss();
                    }
                    activity.finish();
                }
            });
        }

    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ContactsManager.CONTACTS_UPDATED) ||
                    action.equals(BaseManager.AVATAR_LOADED) ||
                    action.equals(ContactsManager.ACTION_USERS_STATUSES_UPDATED)) {
                if (!mContact.iAm) {
                    Contact updatedContact = mContactsManager.getContactById(mContact.getId());
                    if (updatedContact != null) {
                        mContact = updatedContact;
                        updateUi();
                    }
                } else {
                    mContact = mContactsManager.getMyContact();
                    updateUi();
                }
            }
        }
    };

    public ProfileFragment() {
        mContactsManager = ContactsManagerImpl.getInstance();
    }

    public static ProfileFragment getInstance(Contact contact, boolean personal) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(CONTACT, Parcels.wrap(contact));
        arguments.putBoolean(IS_PERSONAL, personal);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mIsPersonalProfile = getArguments().getBoolean(IS_PERSONAL);
        if (!mIsPersonalProfile) {
            mContact = Parcels.unwrap(getArguments().getParcelable(CONTACT));
            if (mContact == null) {
                Toast.makeText(
                        getContext().getApplicationContext(),
                        "something went wrong!", Toast.LENGTH_SHORT
                ).show();
                getActivity().finish();
                return;
            }
            if (mContact.invite && mContact.newInvite) {
                mContactsManager.readInvite(mContact);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        D.log(TAG, "[onActivityCreated]");
        super.onActivityCreated(savedInstanceState);
        if (!mIsPersonalProfile) {
            Utils.setVisibility(mContent, View.VISIBLE);
            Utils.setVisibility(mEmpty, View.GONE);
            Utils.setVisibility(mProgress, View.GONE);
            setHasOptionsMenu(true);
            updateUi();
            getActivity().supportInvalidateOptionsMenu();
        } else {
            getLoaderManager().initLoader(0, null, this).forceLoad();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_profile, container, false);
        mEmpty = (TextView) view.findViewById(android.R.id.empty);
        mContent = (LinearLayout) view.findViewById(R.id.content);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mName = (TextView) view.findViewById(R.id.contact_name);
        mAccountsList = (LinearLayout) view.findViewById(R.id.profile_accounts_list);
        mContactStatus = (TextView) view.findViewById(R.id.contact_status);
        if (mIsPersonalProfile || mContact.shouldHideStatus()) {
            mContactStatus.setVisibility(View.GONE);
        }
        mPersonalStatus = (TextView) view.findViewById(R.id.profile_status);
        mExtraContactsListWrapper =
                (LinearLayout) view.findViewById(R.id.profile_extra_contacts_list_wrapper);
        mAvatarImage = (RoundedImageView) view.findViewById(R.id.contact_avatar);
        mIsDodicallIcon = (ImageView) view.findViewById(R.id.contact_is_dodicall);
        mBlockedWrapper = (LinearLayout) view.findViewById(R.id.profile_blocked_wrapper);
        mUnblockButton = (Button) view.findViewById(R.id.profile_unblock);
        mProfileButtonsWrapper = (RelativeLayout) view.findViewById(R.id.profile_buttons_wrapper);
        mCallButton = (ImageButton) view.findViewById(R.id.profile_button_call);
        mChatButton = (ImageButton) view.findViewById(R.id.profile_button_chat);
        mVideoCallButton = (ImageButton) view.findViewById(R.id.profile_button_video_call);
        mAddToContactWrapper = (LinearLayout) view.findViewById(R.id.add_to_contact_wrapper);
        loadAddToContact();
        setupContactButtons();
        if (mIsPersonalProfile) {
            mBalanceText = (TextView) view.findViewById(R.id.balance);
            mBalanceWrapper = (LinearLayout) view.findViewById(R.id.profile_balance_wrapper);
//            loadBalance(mContactsManager.getBalance());
            view.findViewById(R.id.personal_block_wrapper).setVisibility(View.VISIBLE);
            view.findViewById(R.id.nav_status).setOnClickListener(v ->
                    startActivity(new Intent(
                            getContext().getApplicationContext(), ChangeStatusActivity.class
                    ))
            );
            mContactStatus.setVisibility(View.GONE);
            //TODO: update every 30 seconds
            new AsyncTask<Void, Void, Balance>() {
                String mPayUrl;

                @Override
                protected Balance doInBackground(Void... params) {
                    GlobalApplicationSettingsModel globalSettings = BusinessLogic.GetInstance().GetGlobalApplicationSettings();
                    ServerAreasList serverAreas = BusinessLogic.GetInstance().RetrieveAreas();
                    int serverArea = globalSettings.getArea();
                    ServerAreaModelWrapper areaModel = serverAreas.get(serverArea);

                    mPayUrl = String.format(
                            "%s%s",
                            areaModel.getLcUrl(),
                            appendCountryAndLanguageToUrl(
                                    areaModel.getPayUrl(),
                                    Utils.getLocale(getContext())
                            )
                    );

                    return BL.getBalance();
                }

                @Override
                public void onPostExecute(Balance balance) {
                    setHasOptionsMenu(false);
                    mContactsManager.updateBalance(balance);
                    loadBalance(balance, mPayUrl);
                }

                private String appendCountryAndLanguageToUrl(String url, String locale) {
                    return (
                            url.replaceAll(
                                    "\\$\\{COUNTRY\\}", locale.startsWith("ru") ? "ru" : "gb"
                            ).replaceAll(
                                    "\\$\\{LANG\\}", locale.startsWith("ru") ? "ru" : "en"
                            )
                    );
                }


            }.execute();
        }
        /*
        don't delete
        mExternalAppWrapper = (LinearLayout) view.findViewById(R.id.external_app_links_wrapper);
        */
        ImageButton externalAppToggleButton =
                (ImageButton) view.findViewById(R.id.external_app_links_toggle_button);
//        D.log(TAG, "[onCreateView] fromPhonebook: %s; saved: %s;", mFromPhonebook, mSaved);
        if (mIsPersonalProfile || mContact.isDodicall()) {
            externalAppToggleButton.setVisibility(View.VISIBLE);
            externalAppToggleButton.setOnClickListener(v -> toggleShowExternalApps());
        } else {
            externalAppToggleButton.setVisibility(View.GONE);
        }
        mRequestSentMsg = (TextView) view.findViewById(R.id.request_sent_msg);
        mInviteMsg = (LinearLayout) view.findViewById(R.id.profile_invite_msg);

        /*
        don't delete
        mDevicesList = (LinearLayout) view.findViewById(R.id.profile_devices_list);
        */
        mExtraContactsList = (LinearLayout) view.findViewById(R.id.profile_extra_contacts_list);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcast.registerReceiver(getActivity(), mReceiver,
                ContactsManager.CONTACTS_UPDATED, BaseManager.AVATAR_LOADED, ContactsManager.ACTION_USERS_STATUSES_UPDATED);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcast.unregisterReceiver(getActivity(), mReceiver);
    }

    private void loadRequestMsg() {
        if (mContact.isRequest()) {
            mRequestSentMsg.setVisibility(View.VISIBLE);
        } else {
            mRequestSentMsg.setVisibility(View.GONE);
        }
    }

    private void loadInviteMsg() {
        if (mContact.isInvite()) {
            mInviteMsg.setVisibility(View.VISIBLE);
            mInviteMsg.findViewById(R.id.profile_decline_request).setOnClickListener(this);
            mInviteMsg.findViewById(R.id.profile_accept_request).setOnClickListener(this);
        } else {
            mInviteMsg.setVisibility(View.GONE);
        }
    }

    private void loadBalance(Balance balance, String payUrl) {
        View itemBalance = getView().findViewById(R.id.nav_item_balance);

        if (balance != null) {
            mBalanceWrapper.setVisibility(View.VISIBLE);
            if (balance.isSuccessful()) {
                mBalanceText.setText(Utils.getBalanceText(balance));
                itemBalance.setOnClickListener(v -> Utils.launchUrl(getContext(), payUrl));
            }
        } else {
            mBalanceWrapper.setVisibility(View.GONE);
            itemBalance.setOnClickListener(null);
        }
    }

    private void toggleShowExternalApps() {
        Utils.showComingSoon(getContext());
        /*
        don't delete
        mExternalAppWrapper.setVisibility(
                mExternalAppWrapper.isShown() ? View.GONE : View.VISIBLE
        );
        */
    }

    @Override
    public Loader<Contact> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<Contact>(getActivity()) {
            @Override
            public Contact loadInBackground() {
                return mContactsManager.getMyContact();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Contact> loader, Contact data) {
        D.log(TAG, "[onLoadFinished][profile_debug]");
        mProgress.setVisibility(View.GONE);
        if (data != null) {
            setHasOptionsMenu(true);
            mContent.setVisibility(View.VISIBLE);
            mEmpty.setVisibility(View.GONE);
            mContact = data;
            D.log(TAG, "[onLoadFinished][profile_debug] isRequest: %s", mContact.isRequest());
            updateUi();
            getActivity().supportInvalidateOptionsMenu();
        } else {
            setHasOptionsMenu(false);
            mContent.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Contact> loader) {
        D.log(TAG, "[onLoaderReset] loader_debug");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        D.log(TAG, "[onCreateOptionsMenu]");
        if (!mIsPersonalProfile && !mContact.isFromPhonebook()) {
            if (mContact.isInvite() || mContact.directory) {
                menuInflater.inflate(R.menu.profile_invite, menu);
                menu.findItem(R.id.action_profile_block).setVisible(
                        !mContact.isBlocked() && mContact.id > 0
                );
                return;
            } else {
                menuInflater.inflate(R.menu.profile, menu);
            }
            menuInflater.inflate(R.menu.profile_extra, menu);
            menu.findItem(R.id.action_profile_block).setVisible(!mContact.isBlocked());
            menu.findItem(R.id.action_profile_add_to_white).setTitle(
                    getWhiteMenuItemTitle(mContact.white)
            );
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_profile_block:
                Utils.showConfirm(getContext(), R.string.block_user_confirm, (dialog, which) -> {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        block(true);
                    }
                });
                return true;
            case R.id.action_profile_delete:
                QueryHelper.deleteProfile(getActivity(), mContact);
                return true;
            case R.id.action_profile_edit:
                CreateProfileActivity.start(getContext(), mContact);
                return true;
            case R.id.action_profile_add_to_white:
                setWhite(!mContact.white);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onContactActionSuccess(Contact contact) {
        mContact = contact;
        updateUi();
    }

    @Override
    public void onContactActionFail() {
    }

    private void setWhite(boolean white) {
        mContact = mContactsManager.addToWhiteList(mContact, white);
        Toast.makeText(getActivity(), white ? R.string.contact_added_to_white_list_msg : R.string.contact_removed_from_white_list_msg, Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void loadName() {
        mName.setText(String.format("%s %s", mContact.firstName, mContact.lastName));
    }

    private void loadStatus() {
        if (mIsPersonalProfile) {
            StatusesAdapter.setupStatusView(mPersonalStatus, mContact.getStatus(), mContact.getExtraStatus());
        } else {
            if (mContact.shouldHideStatus()) {
                mContactStatus.setVisibility(View.GONE);
            } else {
                mContactStatus.setVisibility(View.VISIBLE);
                StatusesAdapter.setupStatusView(mContactStatus, mContact.getStatus(), mContact.getExtraStatus());
                final int drawableLevel = StatusesAdapter.getStatusDrawableLevel(mContact.getStatus());
                mCallButton.setImageLevel(drawableLevel);
                mVideoCallButton.setImageLevel(drawableLevel);
                mChatButton.setImageLevel(drawableLevel);
            }
        }
    }

    private void loadAccounts() {
        mAccountsList.removeAllViews();
        if (mContact.sips == null) {
            return;
        }
        int last = -1;
        for (final String number : mContact.sips) {
            D.log(TAG, "[loadAccounts] sipNumber: %s", number);
            if (!TextUtils.isEmpty(number)) {
                String realNumber = TextUtils.split(number, "@")[0];
                final boolean favorite = number.startsWith(DataProvider.FAVORITE_MARKER);
                if (favorite) {
                    realNumber = realNumber.replace(DataProvider.FAVORITE_MARKER, "");
                }
                final ProfileAccountItem item = new ProfileAccountItem(
                        getContext(), ProfileAccountItem.TYPE_SIP, mContact, realNumber,
                        favorite, mIsPersonalProfile, (mContact.directory || mContact.isRequest())
                );
                item.setStatus(mContact.getStatus());
                if (!mIsPersonalProfile) {
                    item.setOnClickListener(v -> {
                        if (!((ProfileAccountItem) v).isChecked()) {
                            saveFavorite(number);
                        }
                    });
                }
                mAccountsList.addView(item);
                last++;
            }
        }
        if (last >= 0) {
            mAccountsList.getChildAt(last).findViewById(R.id.divider).setVisibility(View.GONE);
        }
    }

    private void saveFavorite(String sipNumber) {
        D.log(TAG, "[saveFavorite][favorite_debug] number: %s", sipNumber);
        mContact = mContactsManager.updateFavorite(mContact, sipNumber);
        updateUi();
    }

    private void loadExtraContacts() {
        mExtraContactsList.removeAllViews();

        int itemsCount = 0;
        int last = -1;

        if (mContact.isNotDodicall() || mContact.subscriptionState == Contact.SUBSCRIPTION_STATE_BOTH || mContact.subscriptionState == Contact.SUBSCRIPTION_STATE_FROM) {
            for (String number : mContact.phones) {
                if (!TextUtils.isEmpty(number)) {
                    mExtraContactsList.addView(
                            new ProfileExtraContactItem(
                                    getContext(),
                                    getString(R.string.profile_extra_contact_type_mobile),
                                    number, mIsPersonalProfile
                            )
                    );
                    itemsCount++;
                    last++;
                }
            }
        }

        mExtraContactsListWrapper.setVisibility(itemsCount > 0 ? View.VISIBLE : View.GONE);
        if (last >= 0) {
            mExtraContactsList.getChildAt(last).findViewById(R.id.divider).setVisibility(View.GONE);
        }
    }

    private void loadAvatar() {
        mAvatarImage.setUrl(mContact.avatarPath);
        if (!mContact.isDodicall()) {
            mIsDodicallIcon.setVisibility(View.GONE);
        } else {
            mIsDodicallIcon.setVisibility(View.VISIBLE);
        }
    }

    @StringRes
    private int getWhiteMenuItemTitle(boolean isWhite) {
        if (!isWhite) {
            return R.string.action_profile_add_to_white;
        } else {
            return R.string.action_profile_remove_from_white;
        }
    }

    private void loadAddToContact() {
        if (!mIsPersonalProfile && mContact.mayAddToContact()) {
            mAddToContactWrapper.setVisibility(View.VISIBLE);
            mAddToContactWrapper.findViewById(R.id.add_to_contact_btn).setOnClickListener(this);
            mAddToContactWrapper.findViewById(R.id.add_to_contact_msg_btn).setOnClickListener(this);
        } else {
            mAddToContactWrapper.setVisibility(View.GONE);
        }
    }

    private void loadBlocked() {
        if (mContact.isBlocked()) {
            mBlockedWrapper.setVisibility(View.VISIBLE);
            mUnblockButton.setOnClickListener(this);
        } else {
            mBlockedWrapper.setVisibility(View.GONE);
        }
    }

    private void updateUi() {
        loadAvatar();
        loadInviteMsg();
        loadRequestMsg();
        loadBlocked();
        loadAddToContact();
        loadStatus();
        setupContactButtons();
        loadAccounts();
        loadName();
        loadExtraContacts();
        getActivity().supportInvalidateOptionsMenu();
    }

    private void setupContactButtons() {
        if (mIsPersonalProfile || mContact.shouldHideStatus()) {
            mProfileButtonsWrapper.setVisibility(View.GONE);
        } else {
            mProfileButtonsWrapper.setVisibility(View.VISIBLE);
            mCallButton.setOnClickListener(this);
            mVideoCallButton.setOnClickListener(this);
            mChatButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_button_chat: {
                CreateChatAsyncTask.execute(getActivity(), mContact, this);
                break;
            }
            case R.id.profile_button_call: {
                OutgoingCallActivity.start(getActivity(), mContact);
                break;
            }
            case R.id.profile_button_video_call:
            case R.id.add_to_contact_msg_btn:
                Utils.showComingSoon(getContext());
                break;
            case R.id.profile_unblock:
                v.setEnabled(false);
                block(false);
                break;
            case R.id.profile_accept_request:
                ContactActionAsyncTask.getAcceptAsyncTask(mContactsManager, mContact, this).execute();
                break;
            case R.id.profile_decline_request:
                ContactActionAsyncTask.getDeclineAsyncTask(mContactsManager, mContact, this).execute();
                break;
            case R.id.add_to_contact_btn:
                ContactActionAsyncTask.getSaveAsyncTask(mContactsManager, mContact, this).execute();
                break;
        }
    }

    private void block(boolean blockValue) {
        mContact = mContactsManager.blockContact(mContact, blockValue);
        Toast.makeText(getActivity(), blockValue ? R.string.contact_blocked_msg : R.string.contact_unblocked_msg, Toast.LENGTH_SHORT).show();
        updateUi();
    }

    @Override
    public void onChatCreated(Chat chat) {
        startActivity(ChatActivity.newIntent(getActivity(), chat));
    }
}
