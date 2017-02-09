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
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.parceler.Parcels;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.BaseFragment;
import ru.swisstok.dodicall.fragment.CallHistoryDetailFragment;
import ru.swisstok.dodicall.util.NotificationsUtils;

public class CallHistoryDetailActivity extends BaseActivity implements BaseFragment.FragmentActionListener {

    private static final String EXTRA_CALL_HISTORY = "extraCallHistory";

    private CallHistory mCallHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_history_detail);
        setupActionBar();

        mCallHistory = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CALL_HISTORY));
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, CallHistoryDetailFragment.newInstance(mCallHistory))
                    .commit();
        }
    }

    @Nullable
    @Override
    protected NotificationsUtils.NotificationType getNotificationType() {
        return NotificationsUtils.NotificationType.MissedCall;
    }

    @Override
    protected String getInterestedIdForNotifications() {
        return mCallHistory.id;
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static Intent newIntent(@NonNull Context context, @NonNull CallHistory ch) {
        return new Intent(context, CallHistoryDetailActivity.class).putExtra(EXTRA_CALL_HISTORY, Parcels.wrap(ch));
    }

    public static void start(@NonNull Context context, @NonNull CallHistory ch) {
        context.startActivity(newIntent(context, ch));
    }

    @Override
    public void onContactCall(Contact contact) {
        OutgoingCallActivity.start(this, contact);
    }

    @Override
    public void onOpenContact(Contact contact) {
        ProfileActivity.openProfile(this, contact);
    }

    @Override
    public void onCallToNumber(String number) {
        OutgoingCallActivity.start(this, number);
    }

    @Override
    public void onOpenHistory(CallHistory callHistory) {
    }

    @Override
    public void onOpenChat(Chat chat) {
    }
}
