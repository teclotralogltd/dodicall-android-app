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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.parceler.Parcels;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.SimpleDividerItemDecoration;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class SelectContactsForConferenceActivity extends BaseActivity {

    public static final String EXTRA_CONTACTS = "contacts";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private ArrayList<Contact> mContacts;

    private static class ContactUI {
        boolean selected = false;

        ContactUI(boolean selected) {
            this.selected = selected;
        }
    }

    private ContactUI[] mContactsUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contacts_for_conference);
        ButterKnife.bind(this);

        setupActionBar();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mRecyclerView.setAdapter(new Adapter());

        mContacts = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CONTACTS));
        mContactsUI = new ContactUI[mContacts.size()];

        for (int i = 0; i < mContacts.size(); ++i) {
            mContactsUI[i] = new ContactUI(true);
        }

        mRecyclerView.getAdapter().notifyDataSetChanged();
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
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_CONTACTS, Parcels.wrap(selected)));
            }

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.select_check_box)
        CheckBox selectCheckBox;

        @BindView(R.id.contact_avatar)
        RoundedImageView contactAvatar;

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
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (contactUI.selected != isChecked) {
                    contactUI.selected = isChecked;
                }
            });

            checkBox.setChecked(contactUI.selected);
            holder.itemView.setOnClickListener(v -> checkBox.setChecked(!contactUI.selected));
        }

        @Override
        public int getItemCount() {
            return mContacts == null ? 0 : mContacts.size();
        }
    }

    public static Intent newIntent(Context context, @NonNull ArrayList<Contact> contacts) {
        Intent i = new Intent(context, SelectContactsForConferenceActivity.class);

        if (CollectionUtils.isNotEmpty(contacts)) {
            i.putExtra(EXTRA_CONTACTS, Parcels.wrap(contacts));
        }

        return i;
    }

    @NonNull
    public static ArrayList<Contact> extractResult(Intent data) {
        return Parcels.unwrap(data.getParcelableExtra(EXTRA_CONTACTS));
    }
}
