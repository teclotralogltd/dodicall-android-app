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

package ru.swisstok.dodicall.preference.fragment;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;

import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.preference.CustomSwitchPreference;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.D;

public class CommonPreferenceFragment extends BasePreferenceFragment
        implements LoaderManager.LoaderCallbacks<List<Contact>> {

    private static final String TAG = "CommonPreferenceFragment";

    private CustomSwitchPreference mWhiteListSwitchPref;
    private boolean mIsWhiteListEmpty;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        D.log(TAG, "[onActivityCreated]");
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this).forceLoad();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_common);
        setHasOptionsMenu(true);
        mWhiteListSwitchPref = (CustomSwitchPreference) findPreference(Preferences.Fields.PREF_COMMON_WHITE_LIST);
        mWhiteListSwitchPref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (mWhiteListSwitchPref.isChecked()) {
                return true;
            } else {
                if (mIsWhiteListEmpty) {
                    Snackbar.make(getView(), R.string.pref_white_list_empty_message, Snackbar.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.pref_common_white_list_title)
                            .setMessage(R.string.pref_white_list_warning_message)
                            .setPositiveButton(R.string.turn_on, (dialog, which) -> mWhiteListSwitchPref.setChecked(true))
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }
            }
            return false;
        });
    }

    @Override
    public Loader<List<Contact>> onCreateLoader(int id, Bundle args) {
        return new AsyncTaskLoader<List<Contact>>(getActivity()) {
            @Override
            public List<Contact> loadInBackground() {
                return ContactsManagerImpl.getInstance().getContacts(ToolBarSpinnerAdapter.FILTER_WHITE, null, true, true);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<Contact>> loader, List<Contact> data) {
        D.log(TAG, "[onLoadFinished]");
        if (data != null) {
            mIsWhiteListEmpty = data.isEmpty();
            mWhiteListSwitchPref.setSummary(getResources().getQuantityString(R.plurals.white_list_count, data.size(), data.size()));
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Contact>> loader) {
    }
}
