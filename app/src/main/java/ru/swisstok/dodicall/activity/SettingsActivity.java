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

import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.preference.fragment.BasePreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.ChatsPreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.CommonPreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.DebugPreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.HeadersFragment;
import ru.swisstok.dodicall.preference.fragment.InfoPreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.InterfacePreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.TelephonyPreferenceFragment;
import ru.swisstok.dodicall.util.D;

public class SettingsActivity extends BaseActivity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback,
        HeadersFragment.OnHeaderClickListener {

    private static final String TAG = "SettingsActivity";
    public static final String INIT_FRAGMENT_CLASS = "init_fragment_class";

    public static boolean isXLargeTablet() {
        return (Resources.getSystem().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupActionBar(getString(R.string.title_activity_settings));

        if (savedInstanceState == null) {
            maybeStartWithInitFragment(
                    getFragmentManager().findFragmentByTag(HeadersFragment.TAG),
                    getIntent().getStringExtra(INIT_FRAGMENT_CLASS)
            );
        }
    }

    private void maybeStartWithInitFragment(Fragment headersFragment, String initFragmentClass) {
        D.log(TAG, "[maybeStartWithInitFragment] class: %s; tablet: %s;", initFragmentClass, isXLargeTablet());
        if (headersFragment == null) {
            List<HeadersFragment.Header> headers = new ArrayList<>();
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_common), CommonPreferenceFragment.class));
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_telephony), TelephonyPreferenceFragment.class));
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_chats), ChatsPreferenceFragment.class));
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_interface), InterfacePreferenceFragment.class));
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_info), InfoPreferenceFragment.class));
            headers.add(new HeadersFragment.Header(getString(R.string.pref_header_debug), DebugPreferenceFragment.class));
            headersFragment = HeadersFragment.newInstance(headers, initFragmentClass);
        }
        if (isXLargeTablet()) {
            D.log(TAG, "[maybeStartWithInitFragment] init fragment not exist; isTablet");
            getFragmentManager().beginTransaction().add(
                    R.id.headers, headersFragment, HeadersFragment.TAG
            ).commit();
        } else {
            getFragmentManager().beginTransaction().add(
                    R.id.content, headersFragment, HeadersFragment.TAG
            ).commit();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar(String defaultTitle) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(defaultTitle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            D.log(TAG, "[onBackPressed] popBackStack; size: %d", getFragmentManager().getBackStackEntryCount());
            getFragmentManager().popBackStack();
        } else if (isTaskRoot()) {
            D.log(TAG, "[onBackPressed] isTaskRoot");
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        } else {
            D.log(TAG, "[onBackPressed] super");
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        if (isXLargeTablet()) {
            getFragmentManager().beginTransaction().addToBackStack(TAG).add(
                    R.id.headers_content,
                    Fragment.instantiate(this, preference.getFragment(), preference.getExtras())
            ).commit();
            return true;
        } else {
            final Bundle extras = preference.getExtras();
            extras.putString(BasePreferenceFragment.PREF_TITLE, preference.getTitle().toString());
            getFragmentManager().beginTransaction().addToBackStack(TAG).add(
                    preferenceFragment.getId(),
                    Fragment.instantiate(this, preference.getFragment(), extras)
            ).commit();
            return true;
        }
    }

    @Override
    public void onHeaderClick(HeadersFragment.Header header) {
        D.log(TAG, "[onHeaderClick] header.title: %s; header.fragmentClass: %s", header.title, header.fragmentClass);
        final Bundle args = new Bundle(1);
        args.putString(BasePreferenceFragment.PREF_TITLE, header.title);
        if (isXLargeTablet()) {
            getFragmentManager().beginTransaction().add(
                    R.id.headers_content,
                    Fragment.instantiate(
                            this, header.fragmentClass.getName(), args
                    )
            ).commit();
        } else {
            getFragmentManager().beginTransaction().addToBackStack(TAG).add(
                    R.id.content,
                    Fragment.instantiate(
                            this, header.fragmentClass.getName(), args
                    )
            ).commit();
        }
    }

}
