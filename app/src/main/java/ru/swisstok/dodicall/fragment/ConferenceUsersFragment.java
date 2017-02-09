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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.parceler.Parcels;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class ConferenceUsersFragment extends Fragment {

    public static final String ARGS_CONTACTS = "args.Contacts";

    private ArrayList<Contact> mContacts;

    @BindView(R.id.contacts_grid)
    GridView mContactsGrid;

    public static ConferenceUsersFragment newInstance(@NonNull ArrayList<Contact> contacts) {
        Bundle b = new Bundle(1);
        b.putParcelable(ARGS_CONTACTS, Parcels.wrap(contacts));

        ConferenceUsersFragment frg = new ConferenceUsersFragment();
        frg.setArguments(b);

        return frg;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();

        if (args != null && args.containsKey(ARGS_CONTACTS)) {
            mContacts = Parcels.unwrap(args.getParcelable(ARGS_CONTACTS));
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conference_users, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mContactsGrid.setAdapter(new Adapter());
    }

    private class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mContacts.size();
        }

        @Override
        public Contact getItem(int position) {
            return mContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_conference_user, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }

            final ViewHolder holder = (ViewHolder) convertView.getTag();
            final Contact contact = getItem(position);

            holder.avatar.setUrl(contact.avatarPath);
            holder.isDodicall.setVisibility(contact.isDodicall() ? View.VISIBLE : View.GONE);
            holder.name.setText(Utils.formatAccountFullName(contact));

            return convertView;
        }
    }

    public static class ViewHolder {

        @BindView(R.id.contact_avatar)
        RoundedImageView avatar;

        @BindView(R.id.is_dodicall)
        View isDodicall;

        @BindView(R.id.contact_name)
        TextView name;

        public ViewHolder(View itemView) {
            ButterKnife.bind(this, itemView);
        }
    }

}
