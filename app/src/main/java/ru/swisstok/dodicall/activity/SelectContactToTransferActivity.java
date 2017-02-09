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

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.annimon.stream.Stream;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.TabAdapter;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ContactsFragment;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.NotificationsUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class SelectContactToTransferActivity extends MainActivity {

    public static final String CALL_ID = "call_id";
    public static final String EXTRA_DISABLED_CONTACTS_LIST = "extra.DisableContactsList";

    private List<TabAdapter.TabSpec> sTabs = new ArrayList<>();

    private final CallReceiver.EndCallReceiver mEndCallReceiver =
            new CallReceiver.EndCallReceiver() {
                @Override
                public void onEndCall() {
                    finish();
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle b = new Bundle(3);
        b.putParcelable(ContactsFragment.ARG_DISABLED_CONTACTS_LIST, getIntent().getParcelableExtra(EXTRA_DISABLED_CONTACTS_LIST));
        b.putBoolean(ContactsFragment.BUTTONS_CONTACTS_LIST, false);
        b.putBoolean(ContactsFragment.REQUESTS_CONTACTS_LIST, false);
        b.putBoolean(ContactsFragment.INVITES_CONTACTS_LIST, false);

        sTabs.add(new TabAdapter.TabSpec(
                ContactsFragment.class, R.string.tab_title_contacts,
                R.drawable.contacts_tab_ic, b,
                ContactsFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.WithoutNotification
        ));
//        sTabs.add(new TabAdapter.TabSpec(//todo uncomment when requirements will be specified
//                HistoryFragment.class, R.string.tab_title_history,
//                R.drawable.history_tab_ic, null,
//                HistoryFragment.TAB_POSITION,
//                NotificationsUtils.NotificationType.MissedCall
//
//        ));
//        sTabs.add(new TabAdapter.TabSpec(
//                DialpadFragment.class, R.string.tab_title_dialpad,
//                R.drawable.dialpad_connection, null,
//                DialpadFragment.TAB_POSITION,
//                NotificationsUtils.NotificationType.WithoutNotification
//        ));

        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LocalBroadcast.registerReceiver(this, mEndCallReceiver, CallReceiver.EndCallReceiver.FILTER);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(this, mEndCallReceiver);
    }

    @Override
    protected void setupDrawer(Toolbar toolbar) {
    }

    @Override
    protected void setupTabs(TabLayout tabLayout) {
        super.setupTabs(tabLayout);
        tabLayout.setVisibility(View.GONE);
    }

    @Override
    protected List<TabAdapter.TabSpec> getTabs() {
        return sTabs;
    }

    @Override
    protected boolean isShowCounters() {
        return false;
    }

    @Override
    protected TabLayout.Tab getChatTab() {
        return null;
    }

    @Override
    protected TabLayout.Tab getDialpadTab() {
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContactCall(Contact contact) {
        selectNumberToTransfer(contact);
    }

    @Override
    public void onOpenContact(Contact contact) {
        selectNumberToTransfer(contact);
    }

    @Override
    public void onCallToNumber(String number) {
        startTransfer(number);
    }

    @Override
    public void onOpenHistory(CallHistory callHistory) {
        if (callHistory.contact != null) {
            selectNumberToTransfer(callHistory.contact);
        }
    }

    private void selectNumberToTransfer(Contact contact) {
        final ArrayAdapter<NumberWrapper> numbersAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        int selectPos = 0;
        int j = 0;

        for (String sip : contact.sips) {
            if (!TextUtils.isEmpty(sip)) {
                j++;
                NumberWrapper number = new NumberWrapper(sip, NumberWrapper.TYPE_SIP);
                if (number.isFavorite) {
                    selectPos = j;
                }
                numbersAdapter.add(number);
            }
        }
        Stream.of(contact.phones).filter(phone -> !TextUtils.isEmpty(phone)).forEach(phone ->
                numbersAdapter.add(new NumberWrapper(phone, NumberWrapper.TYPE_PHONE)));

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_number_dialog_title)
                .setSingleChoiceItems(numbersAdapter, selectPos, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final ListView listView = ((AlertDialog) dialog).getListView();
                    final int pos = listView.getCheckedItemPosition();
                    startTransfer(((NumberWrapper) listView.getAdapter().getItem(pos)).callNumber);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startTransfer(String number) {
        startTransfer(getIntent().getStringExtra(CALL_ID), number);
    }

    private void startTransfer(String callId, String number) {
        BusinessLogic.GetInstance().TransferCallToUrl(callId, number);
        finish();
    }

    private static class NumberWrapper {

        @IntDef({TYPE_SIP, TYPE_PHONE})
        @Retention(RetentionPolicy.SOURCE)
        private @interface NumberType {
        }

        private static final int TYPE_SIP = 100;
        private static final int TYPE_PHONE = 101;

        private String callNumber;
        private String displayNumber;
        private boolean isFavorite;

        private NumberWrapper(@NonNull String number, @NumberType int type) {
            if (type == TYPE_SIP) {
                isFavorite = number.startsWith(DataProvider.FAVORITE_MARKER);
                if (isFavorite) {
                    callNumber = number.replace(DataProvider.FAVORITE_MARKER, "");
                } else {
                    callNumber = number;
                }
                displayNumber = TextUtils.split(callNumber, "@")[0];
            } else {
                callNumber = number;
                displayNumber = number;
            }
        }

        @Override
        public String toString() {
            return displayNumber;
        }

    }
}
