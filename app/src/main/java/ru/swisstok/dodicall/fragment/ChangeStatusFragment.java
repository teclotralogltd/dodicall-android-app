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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BaseUserStatus;

public class ChangeStatusFragment extends Fragment implements AdapterView.OnItemClickListener {

    public static final String TAG = "ChangeStatusFragment";

    private static final Integer[] STATUSES = {
            ContactStatus.STATUS_ONLINE,
            ContactStatus.STATUS_DND,
            /*BaseUserStatus.BaseUserStatusAway,*/
            ContactStatus.STATUS_HIDDEN,
            ContactStatus.STATUS_OFFLINE
    };

    @BindView(R.id.extra_status_text)
    EditText mExtraStatusText;

    @BindView(R.id.status_list)
    ListView mStatusesList;

    private StatusesAdapter mStatusesAdapter;

    public ChangeStatusFragment() {
    }

    public static ChangeStatusFragment getInstance() {
        return new ChangeStatusFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.change_status_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ButterKnife.bind(this, getView());

        mExtraStatusText.setOnEditorActionListener((v, actionId, event) -> {
            if (EditorInfo.IME_ACTION_DONE == actionId) {
                saveStatusAndExit();
            }
            return false;
        });

        Contact contact = ContactsManagerImpl.getInstance().getMyContact();
        mStatusesAdapter = new StatusesAdapter(getContext(), R.layout.status_list_item, STATUSES, contact.getStatus());
        mStatusesList.setAdapter(mStatusesAdapter);
        mStatusesList.setOnItemClickListener(this);
        mExtraStatusText.setText(contact.getExtraStatus());

        new Handler().postDelayed(() -> mStatusesAdapter.notifyDataSetChanged(), 100L);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.change_status, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_change_status) {
            saveStatusAndExit();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mStatusesAdapter.setSelectedStatus(mStatusesAdapter.getItem(position));
        mStatusesAdapter.notifyDataSetChanged();
    }

    private void saveStatusAndExit() {
        String extraStatus = mExtraStatusText.getText().toString();
        int status = mStatusesAdapter.getSelectedStatus();

        D.log(TAG, "[saveStatus] status: %s; extraStatus: %s;", status, extraStatus);
        new Thread(() -> BL.saveStatus(BaseUserStatus.swigToEnum(status), extraStatus)).start();

        getActivity().finish();
    }
}
