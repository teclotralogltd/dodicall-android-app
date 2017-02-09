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
import android.widget.Toast;

import ru.swisstok.dodicall.R;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.DeviceSettingsModel;
import ru.uls_global.dodicall.ServerSettingType;
import ru.uls_global.dodicall.ServerSettingsList;

public class AccountsListPreferenceFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_screen_stub);
        setHasOptionsMenu(true);
        BusinessLogic logic = BusinessLogic.GetInstance();
        DeviceSettingsModel deviceSettings = logic.GetDeviceSettings();
        ServerSettingsList serverSettings = deviceSettings.getServerSettings();
        if (serverSettings != null && serverSettings.size() > 0) {
            for (int i = 0; i < serverSettings.size(); i++) {
                if (serverSettings.get(i).getServerType() == ServerSettingType.ServerTypeSip) {
                    Preference preference = new Preference(getActivity(), null, R.style.AppTheme, R.style.Preference_Material);
                    String account = String.format("%s@%s", serverSettings.get(i).getAuthUserName(), serverSettings.get(i).getDomain());
                    preference.setTitle(account);

                    preference.getExtras().putString(AccountPreferenceFragment.SERVER_NAME, serverSettings.get(i).getServer());
                    preference.getExtras().putBoolean(AccountPreferenceFragment.IS_DEFAULT, serverSettings.get(i).getDefault());
                    preference.setFragment(AccountPreferenceFragment.class.getName());
                    getPreferenceScreen().addPreference(preference);
                }
            }
        } else {
            //empty text
            Toast.makeText(getActivity().getApplicationContext(), "Accounts not found!", Toast.LENGTH_LONG).show();
        }
    }

}
