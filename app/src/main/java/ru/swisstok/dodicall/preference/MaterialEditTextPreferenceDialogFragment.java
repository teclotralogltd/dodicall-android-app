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

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.view.View;
import android.widget.EditText;

public class MaterialEditTextPreferenceDialogFragment extends MaterialPreferenceDialogFragment {

    @SuppressWarnings("unused")
    private static final String TAG = "MaterialEditTextPreferenceDialogFragment";
    private EditText mEditText;

    public static MaterialEditTextPreferenceDialogFragment newInstance(String key) {
        final MaterialEditTextPreferenceDialogFragment fragment =
                new MaterialEditTextPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mEditText = (EditText) view.findViewById(android.R.id.edit);
        if (mEditText == null) {
            throw new IllegalStateException(
                    "Dialog view must contain an EditText with id @android:id/edit"
            );
        }
        mEditText.setText(getEditTextPreference().getText());
    }

    private EditTextPreference getEditTextPreference() {
        return (EditTextPreference) getPreference();
    }

    @Override
    protected boolean needInputMethod() {
        // We want the input method to show, if possible, when dialog is displayed
        return true;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String value = mEditText.getText().toString();
            if (getEditTextPreference().callChangeListener(value)) {
                getEditTextPreference().setText(value);
            }
        }
    }
}
