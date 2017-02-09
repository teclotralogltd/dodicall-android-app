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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.function.Function;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.CreateProfileActivity;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.activity.SelectContactActivity;
import ru.swisstok.dodicall.activity.SelectContactToTransferActivity;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.CallHistoryDetail;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.bl.CallHistoriesList;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.LoadCallHistoriesTaskLoader;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class HistoryFragment extends BaseTabFragment {

    public static final int TAB_POSITION = 1;

    private static final int LOADER_ID_LOAD_CALL_HISTORY = 9051;
    private static final int REQUEST_CODE_SELECT_CONTACT = 232;

    @BindView(R.id.no_data_text)
    TextView mNoDataText;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private static CallHistoriesList sCallHistories = new CallHistoriesList();
    private String mIdentity;

    public HistoryFragment() {
    }

    private LoaderManager.LoaderCallbacks<CallHistoriesList> mCallHistoriesLoaderCallbacks = new LoaderManager.LoaderCallbacks<CallHistoriesList>() {
        @Override
        public Loader<CallHistoriesList> onCreateLoader(int id, Bundle args) {
            return new LoadCallHistoriesTaskLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<CallHistoriesList> loader, CallHistoriesList data) {
            Utils.setVisibility(mProgress, View.GONE);

            if (CollectionUtils.isEmpty(data)) {
                Utils.setVisibility(mNoDataText, View.VISIBLE);
                Utils.setVisibility(mRecyclerView, View.GONE);
            } else {
                Utils.setVisibility(mNoDataText, View.GONE);
                Utils.setVisibility(mRecyclerView, View.VISIBLE);

                sCallHistories = data;
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }

            getLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<CallHistoriesList> loader) {
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, DataProvider.ACTION_HISTORY_UPDATED) ||
                    TextUtils.equals(action, ContactsManager.CONTACTS_UPDATED) ||
                    TextUtils.equals(action, ContactsManager.ACTION_USERS_STATUSES_UPDATED) ||
                    TextUtils.equals(action, BaseManager.AVATAR_LOADED) ||
                    TextUtils.equals(action, ContactsManager.CONTACT_REMOVED)) {
                getLoaderManager().initLoader(LOADER_ID_LOAD_CALL_HISTORY, null, mCallHistoriesLoaderCallbacks);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcast.registerReceiver(getActivity(), mBroadcastReceiver, DataProvider.ACTION_HISTORY_UPDATED, ContactsManager.CONTACTS_UPDATED, ContactsManager.ACTION_USERS_STATUSES_UPDATED, BaseManager.AVATAR_LOADED, ContactsManager.CONTACT_REMOVED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(getActivity(), mBroadcastReceiver);
    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        setCallHistoryRead();
//    }
//

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible && isAdded()) {
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tab_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new HistoryAdapter());
        if (!sCallHistories.isEmpty()) {
            Utils.setVisibility(mProgress, View.GONE);
        }

        getLoaderManager().initLoader(LOADER_ID_LOAD_CALL_HISTORY, null, mCallHistoriesLoaderCallbacks);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        if (getActivity() instanceof SelectContactToTransferActivity) {
            return;
        }
        menuInflater.inflate(R.menu.history_tab, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_history_filter:
            case R.id.action_history_edit:
                Utils.showComingSoon(getActivity());
//                item.setChecked(!item.isChecked());
//                if (item.isChecked()) {
//                    item.setIcon(R.drawable.filter_on);
//                } else {
//                    item.setIcon(R.drawable.filter_off);
//                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.avatar)
        RoundedImageView mAvatar;
        @BindView(R.id.is_dodicall)
        ImageView mIsDodicall;
        @BindView(R.id.button_action_right)
        ImageButton mButtonActionRight;
        @BindView(R.id.button_action_left)
        ImageButton mButtonActionLeft;
        @BindView(R.id.contact_name)
        TextView mContactName;
        @BindView(R.id.missed_count)
        TextView mMissedCount;
        @BindView(R.id.last_action)
        TextView mLastAction;
        @BindView(R.id.incoming_srtp)
        ImageView mIncomingSrtp;
        @BindView(R.id.incoming_success_calls_count)
        TextView mIncomingSuccessCallsCount;
        @BindView(R.id.incoming_missed_calls_count)
        TextView mIncomingMissedCallsCount;
        @BindView(R.id.outgoing_srtp)
        ImageView mOutgoingSrtp;
        @BindView(R.id.outgoing_success_calls_count)
        TextView mOutgoingSuccessCallsCount;
        @BindView(R.id.outgoing_fault_calls_count)
        TextView mOutgoingFaultCallsCount;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CallHistory ch = sCallHistories.get(position);

            if (ch.contact != null) {
                final Contact c = ch.contact;

                Utils.setVisibility(holder.mIsDodicall, c.isDodicall() ? View.VISIBLE : View.GONE);
                holder.mContactName.setText(Utils.formatAccountFullName(c));
                holder.mAvatar.setUrl(c.avatarPath);

                Utils.setupActionButtonsForContact(holder.mButtonActionLeft, holder.mButtonActionRight, c, mFragmentActionListener, new Function<Contact, Void>() {
                    @Override
                    public Void apply(Contact contact) {
                        sCallHistories.get(position).contact = BL.saveContact(contact);
                        notifyDataSetChanged();
                        return null;
                    }
                });
            } else {
                holder.mAvatar.setUrl(null);
                Utils.setVisibility(holder.mIsDodicall, View.GONE);
                holder.mContactName.setText(Utils.extractSip(ch.identity));

                Utils.setupActionButtonsForIdentity(holder.mButtonActionLeft, holder.mButtonActionRight, ch.identity, new Utils.IdentityCallback() {
                    @Override
                    public void onCreateNewContact(String identity) {
                        CreateProfileActivity.start(getActivity(), null, Utils.extractSip(identity));

                    }

                    @Override
                    public void onAddToExistContact(String identity) {
                        mIdentity = Utils.extractSip(identity);
                        startActivityForResult(SelectContactActivity.newIntent(getActivity()), REQUEST_CODE_SELECT_CONTACT);
                    }

                    @Override
                    public void onStartCall(String identity) {
                        OutgoingCallActivity.start(getContext(), identity);
                    }
                });
            }

            if (ch.statistics.numberOfMissedCalls < 2) {
                Utils.setVisibilityGone(holder.mMissedCount);
                holder.mContactName.setTextColor(ContextCompat.getColor(getActivity(),
                        (ch.statistics.numberOfMissedCalls == 1)
                                ? R.color.call_history_missed
                                : R.color.call_history_read));
            } else {
                Utils.setVisibilityVisible(holder.mMissedCount);

                holder.mMissedCount.setText(String.format(Locale.getDefault(), "(%s)", String.valueOf(ch.statistics.numberOfMissedCalls)));
                holder.mContactName.setTextColor(ContextCompat.getColor(getActivity(), R.color.call_history_missed));
            }

            StringBuilder sb = new StringBuilder();
            sb.append(Utils.formatDateTimeShort(ch.detail.startTime.getMillis())).append(", ");

            if (ch.detail.historyStatus == CallHistoryDetail.HISTORY_STATUS_SUCCESS) {
                if (ch.detail.direction == CallHistoryDetail.DIRECTION_OUTGOING) {
                    sb.append(getString(R.string.history_status_outgoing_success));
                } else {
                    sb.append(getString(R.string.history_status_incoming_success));
                }
            } else if (ch.detail.historyStatus == CallHistoryDetail.HISTORY_STATUS_MISSED) {
                sb.append(getString(R.string.history_status_missed));
            } else if (ch.detail.historyStatus == CallHistoryDetail.HISTORY_STATUS_ABORTED) {
                sb.append(getString(R.string.history_status_aborted));
            } else if (ch.detail.historyStatus == CallHistoryDetail.HISTORY_STATUS_DECLINED) {
                sb.append(getString(R.string.history_status_declined));
            }

            holder.mLastAction.setText(sb.toString());

            Utils.setVisibility(holder.mIncomingSrtp, ch.statistics.hasIncomingEncryptedCall ? View.VISIBLE : View.INVISIBLE);
            holder.mIncomingSuccessCallsCount.setText(String.valueOf(ch.statistics.numberOfIncomingSuccessfulCalls));
            holder.mIncomingMissedCallsCount.setText(String.valueOf(ch.statistics.numberOfIncomingUnsuccessfulCalls));

            Utils.setVisibility(holder.mOutgoingSrtp, ch.statistics.hasOutgoingEncryptedCall ? View.VISIBLE : View.INVISIBLE);
            holder.mOutgoingSuccessCallsCount.setText(String.valueOf(ch.statistics.numberOfOutgoingSuccessfulCalls));
            holder.mOutgoingFaultCallsCount.setText(String.valueOf(ch.statistics.numberOfOutgoingUnsuccessfulCalls));

            holder.itemView.setOnClickListener(v -> {
                new Thread(() -> BL.setCallHistoryRead(ch.id)).start();
                mFragmentActionListener.onOpenHistory(ch);
            });
        }

        @Override
        public int getItemCount() {
            return sCallHistories.size();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                Contact contact = SelectContactActivity.extractResult(data);
                CreateProfileActivity.start(getActivity(), contact, mIdentity);
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}