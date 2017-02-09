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

package ru.swisstok.dodicall.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.CallActivity;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.view.DialpadButton;

public class ActiveCallDialpadFragment extends Fragment {

    private static final String TAG = "ActiveCallDialpadFragment";

    @BindView(R.id.text_number)
    TextView mNumberText;

    public ActiveCallDialpadFragment() {
    }

    public static ActiveCallDialpadFragment newInstance() {
        return new ActiveCallDialpadFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_call_dialpad, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        updateNumber(Preferences.get(getActivity()).getString(Preferences.Fields.PREF_DIALPAD_LAST_VAL, ""));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Preferences.get(getActivity()).edit().putString(Preferences.Fields.PREF_DIALPAD_LAST_VAL, mNumberText.getText().toString()).apply();
    }

    @OnClick({
            R.id.button_one,
            R.id.button_two,
            R.id.button_three,
            R.id.button_four,
            R.id.button_five,
            R.id.button_six,
            R.id.button_seven,
            R.id.button_eight,
            R.id.button_nine,
            R.id.button_zero,
            R.id.button_star,
            R.id.button_sharp
    })
    void numberClick(View v) {
        D.log(TAG, "[numberClick]");
        updateNumber(String.format(
                "%s%s", String.valueOf(mNumberText.getText()), ((DialpadButton) v).getNumber())
        );
    }

    @OnLongClick(R.id.button_zero)
    boolean zeroLongClick() {
        updateNumber(String.format("%s%s", String.valueOf(mNumberText.getText()), "+"));
        return true;
    }

    @OnClick(R.id.hide_dialpad)
    void hideDialpad() {
        D.log(TAG, "[hideDialpad] current number: %s", mNumberText.getText().toString());
        Preferences.get(getActivity()).edit().putString(Preferences.Fields.PREF_DIALPAD_LAST_VAL, mNumberText.getText().toString()).apply();
        getActivity().onBackPressed();
    }


    @OnClick(R.id.decline)
    void declineCall() {
        ((CallActivity) getActivity()).declineCall();
    }

    private void updateNumber(String number) {
        mNumberText.setText(number);
    }

    public String getNumber() {
        if (mNumberText != null) {
            return mNumberText.getText().toString();
        }
        return "";
    }

}
