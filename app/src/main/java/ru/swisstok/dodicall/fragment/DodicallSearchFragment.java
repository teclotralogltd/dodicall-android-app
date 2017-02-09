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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ProfileActivity;
import ru.swisstok.dodicall.adapter.ContactAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.task.SearchContactsAsyncTaskLoader;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;

public class DodicallSearchFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<ArrayList<Contact>>, ContactAdapter.ContactActionListener {

    private static final String SEARCH_QUERY = "search_query";
    private static final int MIN_DDC_QUERY_LENGTH = 3;

    @BindView(R.id.progress)
    ProgressBar mSearchProgress;

    @BindView(android.R.id.list)
    RecyclerView mList;

    @BindView(android.R.id.empty)
    TextView mEmpty;

    private Adapter mAdapter;
    private Unbinder mUnbinder;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, BaseManager.AVATAR_LOADED)) {
                Contact contact = (Contact) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    Object item = mAdapter.getItem(i);
                    if (item instanceof Contact) {
                        Contact storedContact = (Contact) item;
                        if (storedContact.dodicallId.equals(contact.dodicallId)) {
                            storedContact.avatarPath = contact.avatarPath;
                            mAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
        }
    };

    public DodicallSearchFragment() {
    }

    public static DodicallSearchFragment getInstance() {
        return new DodicallSearchFragment();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcast.registerReceiver(getActivity(), mBroadcastReceiver, BaseManager.AVATAR_LOADED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(getActivity(), mBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_dodicall_search, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mAdapter = new Adapter(getContext(), new ArrayList<>(), true, this);
        mList.setAdapter(mAdapter);
        mList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }

    @Override
    public Loader<ArrayList<Contact>> onCreateLoader(int id, Bundle args) {
        mList.setVisibility(View.GONE);
        mEmpty.setVisibility(View.GONE);
        mSearchProgress.setVisibility(View.VISIBLE);
        return new SearchContactsAsyncTaskLoader(getContext(), args == null ? null : args.getString(SEARCH_QUERY));
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Contact>> loader, ArrayList<Contact> data) {
        mSearchProgress.setVisibility(View.GONE);

        if (CollectionUtils.isNotEmpty(data)) {
            mList.setVisibility(View.VISIBLE);
            mEmpty.setVisibility(View.GONE);
            mAdapter.updateData(new ArrayList<>(data));
            mAdapter.notifyDataSetChanged();
        } else {
            mList.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        }

        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    public void startSearch(String query) {
        if (!TextUtils.isEmpty(query) && query.length() >= MIN_DDC_QUERY_LENGTH) {
            final Bundle args = new Bundle(1);
            args.putString(SEARCH_QUERY, query);
            getLoaderManager().initLoader(0, args, this);
        } else {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            mEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onContactSelected(Contact contact) {
        ProfileActivity.openProfile(getContext(), contact);
    }

    @Override
    public void onChatWithContact(Contact contact) {

    }

    @Override
    public void onCallToContact(Contact contact) {

    }

    @Override
    public void onAddContact(Contact contact) {
    }

    class Adapter extends ContactAdapter {

        public Adapter(Context context, List<Contact> data, boolean withButtons, ContactActionListener contactActionListener) {
            super(context, new ArrayList<>(data), new ArrayList<>(), withButtons, contactActionListener);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Contact contact = (Contact) getItem(position);
            ContactViewHolder contactHolder = ((ContactViewHolder) holder);

            contactHolder.itemView.setOnClickListener(view -> onContactSelected(contact));

            Picasso.with(getContext())
                    .load(new File(contact.avatarPath))
                    .networkPolicy(NetworkPolicy.NO_STORE)
                    .transform(ROUNDED_TRANSFORMATION)
                    .placeholder(R.drawable.no_photo_user)
                    .into(contactHolder.mRoundedImageView);

            contactHolder.mDodicallContact.setVisibility(View.VISIBLE);
            contactHolder.mContactName.setText(Utils.formatAccountFullName(contact));
            contactHolder.mContactStatus.setVisibility(View.GONE);

            contactHolder.mCallView.setVisibility(View.GONE);
            contactHolder.mChatView.setVisibility(View.GONE);

            if (contact.subscriptionState == Contact.SUBSCRIPTION_STATE_NONE) {
                contactHolder.mAddToContactsView.setVisibility(View.VISIBLE);
                contactHolder.mAddToContactsView.setImageResource(R.drawable.accept_user_request);
                contactHolder.mAddToContactsView.setOnClickListener(view -> {
                    Contact c = ContactsManagerImpl.getInstance().acceptRequest(contact);

                    getData().set(position, c);
                    notifyItemChanged(position);
                });
            } else {
                contactHolder.mAddToContactsView.setVisibility(View.GONE);
            }
        }
    }
}
