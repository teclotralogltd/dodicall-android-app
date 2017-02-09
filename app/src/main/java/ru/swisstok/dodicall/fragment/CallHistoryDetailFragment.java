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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatDrawableManager;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.parceler.Parcels;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.CreateProfileActivity;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.activity.SelectContactActivity;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.CallHistoryDetail;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.CallHistoriesList;
import ru.swisstok.dodicall.bl.CallHistoryDetailsList;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.LoadCallHistoriesTaskLoader;
import ru.swisstok.dodicall.task.LoadCallHistoryDetailsTaskLoader;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class CallHistoryDetailFragment extends Fragment {

    public static final String ARG_CALL_HISTORY = "argCallHistory";
    private static final int LOADER_ID_GET_DETAILS = 8888;
    private static final int LOADER_ID_LOAD_CALL_HISTORY = 8887;
    private static final int REQUEST_CODE_SELECT_CONTACT = 323;

    @BindView(R.id.no_data_text)
    TextView mNoDataText;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
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
    @BindView(R.id.contact_status)
    TextView mContactStatus;
    @BindView(R.id.header)
    RelativeLayout mHeader;

    private CallHistory mCallHistory;
    private CallHistoryDetailsList mData = new CallHistoryDetailsList(0);
    private String mIdentity;
    private BaseFragment.FragmentActionListener mFragmentActionListener;

    private LoaderManager.LoaderCallbacks<CallHistoryDetailsList> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<CallHistoryDetailsList>() {
        @Override
        public Loader<CallHistoryDetailsList> onCreateLoader(int id, Bundle args) {
            return new LoadCallHistoryDetailsTaskLoader(getActivity(), mCallHistory);
        }

        @Override
        public void onLoadFinished(Loader<CallHistoryDetailsList> loader, CallHistoryDetailsList data) {
            setData(data);
            getLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<CallHistoryDetailsList> loader) {
        }
    };

    private LoaderManager.LoaderCallbacks<CallHistoriesList> mCallHistoryLoaderCallbacks = new LoaderManager.LoaderCallbacks<CallHistoriesList>() {
        @Override
        public Loader<CallHistoriesList> onCreateLoader(int id, Bundle args) {
            return new LoadCallHistoriesTaskLoader(getActivity(), mCallHistory.id);
        }

        @Override
        public void onLoadFinished(Loader<CallHistoriesList> loader, CallHistoriesList data) {
            Utils.setVisibility(mProgress, View.GONE);

            if (CollectionUtils.isEmpty(data)) {
                Utils.setVisibility(mNoDataText, View.VISIBLE);
                Utils.setVisibility(mRecyclerView, View.GONE);
            } else {
                mCallHistory = data.get(0);
                setupHeader();
            }

            getLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<CallHistoriesList> loader) {
        }
    };

    private void setData(CallHistoryDetailsList data) {
        Utils.setVisibility(mProgress, View.GONE);

        if (CollectionUtils.isEmpty(data)) {
            Utils.setVisibility(mNoDataText, View.VISIBLE);
            Utils.setVisibility(mRecyclerView, View.GONE);
        } else {
            Utils.setVisibility(mNoDataText, View.GONE);
            Utils.setVisibility(mRecyclerView, View.VISIBLE);

            mData = data;
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof BaseFragment.FragmentActionListener) {
            mFragmentActionListener = (BaseFragment.FragmentActionListener) activity;
        } else {
            throw new ClassCastException(activity.toString() + " must implement BaseFragment.FragmentActionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_call_history_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(new Adapter());

        setupHeader();

        getLoaderManager().initLoader(LOADER_ID_GET_DETAILS, null, mLoaderCallbacks);
    }

    private void setupHeader() {
        CallHistory ch = mCallHistory;

        if (ch.contact != null) {
            final Contact c = ch.contact;

            Utils.setVisibility(mIsDodicall, c.isDodicall() ? View.VISIBLE : View.GONE);
            mContactName.setText(Utils.formatAccountFullName(c));
            mAvatar.setUrl(c.avatarPath);

            if (c.isDodicall() && c.isMine) {
                Utils.setVisibilityVisible(mContactStatus);

                if (!c.blocked) {
                    StatusesAdapter.setupStatusView(mContactStatus, c.getStatus(), c.getExtraStatus());
                } else {
                    mContactStatus.getCompoundDrawables()[0].setLevel(4);
                    mContactStatus.setText(R.string.user_blocked_status);
                }
            } else {
                Utils.setVisibilityGone(mContactStatus);
            }

            Utils.setupActionButtonsForContact(mButtonActionLeft, mButtonActionRight, c, mFragmentActionListener, contact -> {
                mCallHistory.contact = contact;
                setupHeader();
                return null;
            });

            mHeader.setOnClickListener(v -> mFragmentActionListener.onOpenContact(c));
        } else {
            Utils.setVisibility(mIsDodicall, View.GONE);
            Utils.setVisibilityGone(mContactStatus);

            mAvatar.setUrl(null);
            mContactName.setText(Utils.extractSip(ch.identity));

            Utils.setupActionButtonsForIdentity(mButtonActionLeft, mButtonActionRight, ch.identity, new Utils.IdentityCallback() {
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

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, DataProvider.ACTION_HISTORY_UPDATED)) {
                getLoaderManager().initLoader(LOADER_ID_GET_DETAILS, null, mLoaderCallbacks);
            } else if (TextUtils.equals(action, ContactsManager.ACTION_USERS_STATUSES_UPDATED) ||
                    TextUtils.equals(action, ContactsManager.CONTACTS_UPDATED)) {
                getLoaderManager().initLoader(LOADER_ID_LOAD_CALL_HISTORY, null, mCallHistoryLoaderCallbacks);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mCallHistory = Parcels.unwrap(getArguments().getParcelable(ARG_CALL_HISTORY));

        LocalBroadcast.registerReceiver(getActivity(), mBroadcastReceiver, DataProvider.ACTION_HISTORY_UPDATED, ContactsManager.CONTACTS_UPDATED, ContactsManager.ACTION_USERS_STATUSES_UPDATED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(getActivity(), mBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.history_tab, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_history_filter:
            case R.id.action_history_edit:
                Utils.showComingSoon(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.source_image)
        ImageView mSourceImage;
        @BindView(R.id.encrypted)
        ImageView mEncrypted;
        @BindView(R.id.date_text)
        TextView mDateText;
        @BindView(R.id.source_text)
        TextView mSourceText;
        @BindView(R.id.duration_text)
        TextView mDurationText;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = getActivity().getLayoutInflater().inflate(R.layout.item_call_history_detail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final CallHistoryDetail data = mData.get(position);

            Drawable d = null;
            String source = null;

            if (data.historyStatus == CallHistoryDetail.HISTORY_STATUS_SUCCESS) {
                if (data.direction == CallHistoryDetail.DIRECTION_INCOMING) {
                    d = AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_incoming_success);
                    source = getString(R.string.history_status_detail_incoming_success);
                } else {
                    d = AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_outgoing_success);
                    source = getString(R.string.history_status_detail_outgoing_success);
                }
            } else if (data.historyStatus == CallHistoryDetail.HISTORY_STATUS_MISSED) {
                d = AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_incoming_missed);
                source = getString(R.string.history_status_detail_missed);
            } else if (data.historyStatus == CallHistoryDetail.HISTORY_STATUS_DECLINED) {
                d = AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_outgoing_fault);
                source = getString(R.string.history_status_detail_declined);
            } else if (data.historyStatus == CallHistoryDetail.HISTORY_STATUS_ABORTED) {
                d = AppCompatDrawableManager.get().getDrawable(getActivity(), R.drawable.ic_outgoing_fault);
                source = getString(R.string.history_status_detail_aborted);
            }

            holder.mSourceImage.setImageDrawable(d);
            holder.mSourceText.setText(source);

            Utils.setVisibility(holder.mEncrypted, data.encryption == CallHistoryDetail.ENCRYPTION_SRTP
                    ? View.VISIBLE
                    : View.INVISIBLE);


            if (data.durationInSeconds < 61) {
                holder.mDurationText.setText(String.format(Locale.getDefault(), "%d %s", data.durationInSeconds, getString(R.string.duration_sec)));
            } else if (data.durationInSeconds < 3600) {
                holder.mDurationText.setText(String.format(Locale.getDefault(), "%d:%d %s", data.durationInSeconds / 60, data.durationInSeconds % 60, getString(R.string.duration_min)));
            } else {
                int hour = data.durationInSeconds / 3600;
                int min = (data.durationInSeconds % 3600) / 60;
                int sec = (data.durationInSeconds % 3600) % 60;

                holder.mDurationText.setText(String.format(Locale.getDefault(), "%d:%d:%d %s", hour, min, sec, getString(R.string.duration_hour)));
            }

            holder.mDateText.setText(Utils.formatDateTime(getActivity(), data.startTime));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    public static Fragment newInstance(CallHistory ch) {
        Fragment fragment = new CallHistoryDetailFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(ARG_CALL_HISTORY, Parcels.wrap(ch));
        fragment.setArguments(args);

        return fragment;
    }
}
