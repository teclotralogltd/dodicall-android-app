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

package ru.swisstok.dodicall.preference;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;

public class MaterialListPreferenceDialogFragment extends MaterialPreferenceDialogFragment {

    private int mClickedDialogEntryIndex;

    public static MaterialListPreferenceDialogFragment newInstance(String key) {
        final MaterialListPreferenceDialogFragment fragment =
                new MaterialListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        final ListPreference preference = getListPreference();
        if (preference.getEntries() == null || preference.getEntryValues() == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }
        mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
        builder.setSingleChoiceItems(preference.getEntries(), mClickedDialogEntryIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mClickedDialogEntryIndex = which;
                        /*
                         * Clicking on an item simulates the positive button
                         * click, and dismisses the dialog.
                         */
                        MaterialListPreferenceDialogFragment.this.onClick(
                                dialog, DialogInterface.BUTTON_POSITIVE
                        );
                        dialog.dismiss();
                    }
                });
        /*
         * The typical interaction for list-based dialogs is to have
         * click-on-an-item dismiss the dialog instead of the user having to
         * press 'Ok'.
         */
        builder.setPositiveButton(null, null);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        final ListPreference preference = getListPreference();
        if (positiveResult && mClickedDialogEntryIndex >= 0 &&
                preference.getEntryValues() != null) {
            String value = preference.getEntryValues()[mClickedDialogEntryIndex].toString();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }

}
