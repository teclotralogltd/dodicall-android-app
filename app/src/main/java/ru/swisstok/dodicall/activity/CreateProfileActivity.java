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
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.parceler.Parcels;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.EditProfileFragment;

public class CreateProfileActivity extends BaseActivity implements EditProfileFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Contact contact = Parcels.unwrap(getIntent().getParcelableExtra(EditProfileFragment.CONTACT));

        getSupportActionBar().setTitle(contact == null
                ? R.string.title_activity_create_contact
                : R.string.title_activity_edit_profile
        );

        String extraNumber = Parcels.unwrap(getIntent().getParcelableExtra(EditProfileFragment.ARG_NUMBER));

        getSupportFragmentManager().beginTransaction().replace(
                R.id.content,
                EditProfileFragment.getInstance(contact, extraNumber),
                EditProfileFragment.TAG
        ).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_done) {
            getFragment().saveContact();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContactSaved(Contact contact) {
        ProfileActivity.openProfile(this, contact);
        finish();
    }

    private EditProfileFragment getFragment() {
        return (EditProfileFragment) getSupportFragmentManager().findFragmentByTag(EditProfileFragment.TAG);
    }

    public static void start(Context context) {
        start(context, null);
    }

    public static void start(Context context, @Nullable Contact contact) {
        start(context, contact, null);
    }

    public static void start(Context context, @Nullable Contact contact, @Nullable String extraNumber) {
        context.startActivity(
                new Intent(context.getApplicationContext(), CreateProfileActivity.class)
                        .putExtra(EditProfileFragment.CONTACT, Parcels.wrap(contact))
                        .putExtra(EditProfileFragment.ARG_NUMBER, Parcels.wrap(extraNumber))
        );
    }
}
