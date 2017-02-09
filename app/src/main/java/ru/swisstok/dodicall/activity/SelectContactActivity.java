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

package ru.swisstok.dodicall.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.task.ContentResolverAsyncTaskLoader;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.SimpleDividerItemDecoration;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class SelectContactActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<Contact>> {
    private static final int LOADER_ID_LOAD_CONTACTS = 1;
    private static final String EXTRA_CONTACT = "extraContact";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.no_data_text)
    TextView mNoDataText;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private List<Contact> mContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact);
        ButterKnife.bind(this);

        setupActionBar();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mRecyclerView.setAdapter(new Adapter());

        getSupportLoaderManager().initLoader(LOADER_ID_LOAD_CONTACTS, null, this);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<Contact>> onCreateLoader(int id, Bundle args) {
        return new ContentResolverAsyncTaskLoader<List<Contact>>(this) {
            @Override
            public List<Contact> loadInBackground(@NonNull ContentResolver contentResolver) {
                List<Contact> result = ContactsManagerImpl.getInstance().getContacts(ToolBarSpinnerAdapter.FILTER_ALL, null, false, true);

                if (result == null) {
                    return new ArrayList<>(0);
                }

                Collections.sort(result, (lhs, rhs) -> lhs.firstName.compareToIgnoreCase(rhs.firstName));
                return result;
            }

            @Override
            protected void onStartLoading() {
                forceLoad();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<Contact>> loader, List<Contact> data) {
        mProgressBar.setVisibility(View.GONE);

        if (CollectionUtils.isEmpty(data)) {
            mNoDataText.setVisibility(View.VISIBLE);
        } else {
            mContacts = data;

            mNoDataText.setVisibility(View.GONE);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Contact>> loader) {

    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.contact_avatar)
        RoundedImageView contactAvatar;

        @BindView(R.id.contact_is_dodicall)
        ImageView isDodicall;

        @BindView(R.id.name)
        TextView nameText;

        @BindView(R.id.status_text)
        TextView statusText;

        ContactViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class Adapter extends RecyclerView.Adapter<ContactViewHolder> {

        @Override
        public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.choose_contact_item, parent, false);
            return new ContactViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ContactViewHolder holder, final int position) {
            final Contact item = mContacts.get(position);

            holder.nameText.setText(Utils.formatAccountFullName(item));
            holder.contactAvatar.setUrl(item.avatarPath);

            if (item.isDodicall()) {
                Utils.setVisibilityVisible(holder.statusText);
                Utils.setVisibilityVisible(holder.isDodicall);

                StatusesAdapter.setupStatusView(holder.statusText, item.getStatus(), item.getExtraStatus());
            } else {
                Utils.setVisibilityGone(holder.statusText);
                Utils.setVisibilityGone(holder.isDodicall);
            }

            holder.itemView.setOnClickListener(v -> {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_CONTACT, Parcels.wrap(item)));
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return mContacts == null ? 0 : mContacts.size();
        }
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, SelectContactActivity.class);
    }

    @Nullable
    public static Contact extractResult(Intent data) {
        return Parcels.unwrap(data.getParcelableExtra(EXTRA_CONTACT));
    }
}
