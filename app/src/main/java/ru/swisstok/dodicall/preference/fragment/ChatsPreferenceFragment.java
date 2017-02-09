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
import ru.swisstok.dodicall.preference.SeekBarPreference;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Utils;

public class ChatsPreferenceFragment extends BasePreferenceFragment {

    private static final String TAG = "ChatsPreferenceFragment";

    public static final String PREF_CHATS_CLEAR = "pref_chats_clear";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_chats);
        setHasOptionsMenu(true);
        findPreference(PREF_CHATS_CLEAR).setOnPreferenceClickListener(preference -> {
            Utils.showComingSoon(getActivity());
            return true;
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        D.log(TAG, "[onDisplayPreferenceDialog]");
        if (preference instanceof SeekBarPreference) {
            D.log(TAG, "[onDisplayPreferenceDialog] show seekbar");
            SeekBarPreference.SeekBarPreferenceDialogFragment dialog =
                    SeekBarPreference.SeekBarPreferenceDialogFragment.newInstance(
                            preference.getKey()
                    );
            dialog.setTargetFragment(this, 0);
            dialog.show(getFragmentManager(), null);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

}
