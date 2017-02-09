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

import android.os.Bundle;
import android.support.v7.preference.Preference;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.WebActivity;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.Utils;

public class InfoPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_info);
        setHasOptionsMenu(true);
        findPreference(Preferences.Fields.PREF_INFO_ABOUT).setOnPreferenceClickListener(this);
        findPreference(Preferences.Fields.PREF_INFO_WHAT_NEW).setOnPreferenceClickListener(this);
        findPreference(Preferences.Fields.PREF_INFO_KNOWN_ISSUES).setOnPreferenceClickListener(this);
        findPreference(Preferences.Fields.PREF_INFO_HELP).setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case Preferences.Fields.PREF_INFO_ABOUT:
                WebActivity.launchWeb(
                        getActivity(),
                        getString(R.string.pref_info_about_url),
                        preference.getTitle().toString()
                );
                return true;
            case Preferences.Fields.PREF_INFO_WHAT_NEW:
                WebActivity.launchWeb(
                        getActivity(),
                        getString(R.string.pref_info_what_new_url),
                        preference.getTitle().toString()
                );
                return true;
            case Preferences.Fields.PREF_INFO_KNOWN_ISSUES:
                WebActivity.launchWeb(
                        getActivity(),
                        getString(R.string.pref_info_known_issues_url),
                        preference.getTitle().toString()
                );
                return true;
            case Preferences.Fields.PREF_INFO_HELP:
                /*WebActivity.launchWeb(
                        getActivity(),
                        getString(R.string.pref_info_help_url),
                        preference.getTitle().toString()
                );*/
                Utils.showComingSoon(getActivity());
                return true;
            default:
                return false;
        }
    }

}
