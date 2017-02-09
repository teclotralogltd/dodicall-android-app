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
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.parceler.Parcels;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ProfileFragment;
import ru.swisstok.dodicall.util.D;

public class ProfileActivity extends BaseActivity {

    @SuppressWarnings("unused")
    private static final String TAG = "ProfileActivity";

    public static final String ACTION_OPEN_PERSONAL_PROFILE =
            "ru.swisstok.dodicall.action.open_personal_profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Contact contact = Parcels.unwrap(getIntent().getParcelableExtra(ProfileFragment.CONTACT));
        boolean isPersonal = TextUtils.equals(
                getIntent().getAction(), ACTION_OPEN_PERSONAL_PROFILE
        );
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            //for locale switch
            if (isPersonal) {
                actionBar.setTitle(R.string.title_activity_my_profile);
            } else {
                actionBar.setTitle(R.string.title_activity_profile);
            }
        }
        D.log(TAG, "isPersonal: %s", isPersonal);
        getSupportFragmentManager().beginTransaction().replace(
                R.id.content,
                ProfileFragment.getInstance(contact, isPersonal),
                ProfileFragment.TAG
        ).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    public static void openProfile(Context context, Contact contact) {
        context.startActivity(newIntent(context, contact));
    }

    public static Intent newIntent(Context context, Contact contact) {
        return new Intent(context.getApplicationContext(), ProfileActivity.class)
                .putExtra(ProfileFragment.CONTACT, Parcels.wrap(contact));
    }
}
