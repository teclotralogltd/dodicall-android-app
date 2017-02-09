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

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.preference.AccountCustomSwitchPreference;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.D;

public class AccountPreferenceFragment extends BasePreferenceFragment {

    private static final String TAG = "AccountPreferenceFragment";

    public static final String SERVER_NAME = "server_name";
    public static final String IS_DEFAULT = "is_default";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_screen_stub);
        setHasOptionsMenu(true);
        String server = getArguments().getString(SERVER_NAME);
        boolean isDefault = getArguments().getBoolean(IS_DEFAULT);
        D.log(TAG, "default: %s", isDefault);
        AccountCustomSwitchPreference accountPreference = new AccountCustomSwitchPreference(
                getActivity(), server, isDefault
        );
        accountPreference.setTitle(R.string.pref_telephony_account_default_title);
        accountPreference.setKey(Preferences.Fields.PREF_TELEPHONY_ACCOUNT_DEFAULT);
        accountPreference.setTableColumn("DefaultVoipServer");
        getPreferenceScreen().addPreference(accountPreference);
    }

}
