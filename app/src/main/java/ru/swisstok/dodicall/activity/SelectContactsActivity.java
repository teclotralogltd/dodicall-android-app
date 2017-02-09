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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

public class SelectContactsActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<ArrayList<Contact>> {

    public static final String EXTRA_CONTACTS = "contacts";
    public static final String EXTRA_DISABLE_SELECTED_CONTACT = "disableSelectedContacts";

    private static final int LOADER_ID_LOAD_CONTACTS = 1;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.progress_bar)
    ProgressBar mProgressBar;

    @BindView(R.id.no_data_text)
    TextView mNoDataText;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private ArrayList<Contact> mContacts;
    private ArrayList<Contact> mNotMyContacts;

    private static class ContactUI {
        boolean selected = false;
        boolean disable = false;

        ContactUI() {
        }
    }

    private ContactUI[] mContactsUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_contacts, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else if (id == R.id.action_done) {
            if (mContactsUI == null) {
                setResult(RESULT_CANCELED);
            } else {
                ArrayList<Contact> selected = new ArrayList<>();
                for (int i = 0; i < mContactsUI.length; i++) {
                    if (mContactsUI[i].selected) {
                        selected.add(mContacts.get(i));
                    }
                }

                if (CollectionUtils.isNotEmpty(mNotMyContacts)) {
                    selected.addAll(mNotMyContacts);
                }

                setResult(RESULT_OK, new Intent().putExtra(EXTRA_CONTACTS, Parcels.wrap(selected)));
            }

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<ArrayList<Contact>> onCreateLoader(int id, Bundle args) {
        return new ContentResolverAsyncTaskLoader<ArrayList<Contact>>(this) {
            @Override
            public ArrayList<Contact> loadInBackground(@NonNull ContentResolver contentResolver) {
                ArrayList<Contact> result = new ArrayList<>();
                List<Contact> dodicallContacts = ContactsManagerImpl.getInstance().getContacts(ToolBarSpinnerAdapter.FILTER_DDC, null, false, true);
                List<Contact> blockedContacts = ContactsManagerImpl.getInstance().getContacts(ToolBarSpinnerAdapter.FILTER_BLOCKED, null, false, true);

                if (CollectionUtils.isNotEmpty(dodicallContacts)) {
                    result.addAll(dodicallContacts);
                }

                if (CollectionUtils.isNotEmpty(blockedContacts)) {
                    for (Contact c : blockedContacts) {
                        if (c.isDodicall()) {
                            result.add(c);
                        }
                    }
                }

                result = Stream.of(result).filter(value -> !value.isInvite() && !value.subscriptionRequest).collect(Collectors.toCollection(ArrayList::new));

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
    public void onLoadFinished(Loader<ArrayList<Contact>> loader, ArrayList<Contact> data) {
        mProgressBar.setVisibility(View.GONE);

        if (CollectionUtils.isEmpty(data)) {
            mNoDataText.setVisibility(View.VISIBLE);
        } else {
            mContacts = data;
            mContactsUI = new ContactUI[mContacts.size()];

            ArrayList<Contact> preSelected = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CONTACTS));
            boolean disableSelectedContact = getIntent().getBooleanExtra(EXTRA_DISABLE_SELECTED_CONTACT, false);

            for (int i = 0; i < mContacts.size(); ++i) {
                ContactUI contactUI = new ContactUI();

                if (CollectionUtils.isNotEmpty(preSelected)) {
                    final Contact contact = mContacts.get(i);
                    final Iterator<Contact> it = preSelected.iterator();

                    while (it.hasNext()) {
                        Contact c = it.next();
                        if (c.id == contact.id) {
                            it.remove();
                            contactUI.selected = true;
                            contactUI.disable = disableSelectedContact;
                            break;
                        }
                    }
                }

                mContactsUI[i] = contactUI;
            }

            if (CollectionUtils.isNotEmpty(preSelected)) {
                mNotMyContacts = new ArrayList<>();
                mNotMyContacts.addAll(preSelected);
            }

            mNoDataText.setVisibility(View.GONE);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Contact>> loader) {

    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.select_check_box)
        CheckBox selectCheckBox;

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
            View v = getLayoutInflater().inflate(R.layout.contact_item, parent, false);
            return new ContactViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ContactViewHolder holder, final int position) {
            final Contact item = mContacts.get(position);
            final CheckBox checkBox = holder.selectCheckBox;

            holder.nameText.setText(Utils.formatAccountFullName(item));
            holder.contactAvatar.setUrl(item.avatarPath);
            StatusesAdapter.setupStatusView(holder.statusText, item.getStatus(), item.getExtraStatus());

            final ContactUI contactUI = mContactsUI[position];


            if (!contactUI.disable) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (contactUI.selected != isChecked) {
                        contactUI.selected = isChecked;
                    }
                });
            }
            checkBox.setChecked(contactUI.selected);

            if (!contactUI.disable) {
                holder.itemView.setOnClickListener(v -> checkBox.setChecked(!contactUI.selected));
            }

            checkBox.setEnabled(!contactUI.disable);

            float alpha = contactUI.disable ? .3f : 1.f;

            holder.contactAvatar.setAlpha(alpha);
            holder.nameText.setAlpha(alpha);
            holder.selectCheckBox.setAlpha(alpha);
            holder.statusText.setAlpha(alpha);
            holder.isDodicall.setAlpha(alpha);
        }

        @Override
        public int getItemCount() {
            return mContacts == null ? 0 : mContacts.size();
        }
    }

    public static Intent newIntent(Context context, @Nullable ArrayList<Contact> contacts) {
        return newIntent(context, contacts, false);
    }

    public static Intent newIntent(Context context, @Nullable ArrayList<Contact> contacts, boolean disableSelectedContacts) {
        Intent i = new Intent(context, SelectContactsActivity.class);

        if (CollectionUtils.isNotEmpty(contacts)) {
            i.putExtra(EXTRA_CONTACTS, Parcels.wrap(contacts));
        }

        i.putExtra(EXTRA_DISABLE_SELECTED_CONTACT, disableSelectedContacts);

        return i;
    }

    public static Intent newIntent(Context context) {
        return newIntent(context, null, false);
    }

    @Nullable
    public static ArrayList<Contact> extractResult(Intent data) {
        return Parcels.unwrap(data.getParcelableExtra(EXTRA_CONTACTS));
    }
}
