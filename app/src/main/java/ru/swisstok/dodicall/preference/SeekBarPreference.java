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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BusinessLogic;

public class SeekBarPreference extends DialogPreference implements SaveDbPreference {

    private static final String TAG = "SEEK_BAR_PREFERENCE";

    private static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String APP_NAMESPACE = "http://schemas.android.com/apk/res-auto";

    private static final String ATTR_DEFAULT_VALUE = "defaultValue";
    private static final String ATTR_MIN_VALUE = "minValue";
    private static final String ATTR_MAX_VALUE = "maxValue";
    private static final String ATTR_TABLE_COLUMN = "tableColumn";

    private static final int DEFAULT_MIN = 0;
    private static final int DEFAULT_MAX = 1000;
    private static final int DEFAULT_CURRENT = 500;

    private int mDefaultValue;
    private int mMinValue;
    private int mMaxValue;
    private String mTableColumn;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public int getMinValue() {
        return mMinValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public int getDefaultValue() {
        return mDefaultValue;
    }

    @Override
    public String getSummary() {
        return String.valueOf(getPersistedInt(0));
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ru_swisstok_preference_SeekBarPreference
        );
        mDefaultValue = attrs.getAttributeIntValue(
                ANDROID_NAMESPACE, ATTR_DEFAULT_VALUE, DEFAULT_CURRENT
        );
        //TODO: add support for integer resource
        //TODO: try-catch?
        mMinValue = attrs.getAttributeIntValue(APP_NAMESPACE, ATTR_MIN_VALUE, DEFAULT_MIN);
        mMaxValue = attrs.getAttributeIntValue(APP_NAMESPACE, ATTR_MAX_VALUE, DEFAULT_MAX);
        mTableColumn = attrs.getAttributeValue(APP_NAMESPACE, ATTR_TABLE_COLUMN);
        D.log(TAG, "[init] minValue: %s; maxValue: %s;", mMinValue, mMaxValue);
        a.recycle();
    }

    @Override
    protected int getPersistedInt(int defaultReturnValue) {
        switch (getKey()) {
            case Preferences.Fields.PREF_CHATS_FONT_SIZE:
                return BusinessLogic.GetInstance().GetUserSettings().getGuiFontSize();
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
    }

    @Override
    protected boolean persistInt(int value) {
        D.log(TAG, "[persistString] value: %s", value);
        if (value == getPersistedInt(0)) {
            return false;
        }
        switch (getKey()) {
            case Preferences.Fields.PREF_CHATS_FONT_SIZE:
                D.log(TAG, "[persistString] font_size");
                BusinessLogic.GetInstance().SaveUserSettings(
                        getTableColumn(), value
                );
                return true;
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
    }

    @Override
    public String getTableColumn() {
        return mTableColumn;
    }

    @Override
    public void setTableColumn(String columnName) {
        mTableColumn = columnName;
    }

    public static class SeekBarPreferenceDialogFragment extends MaterialPreferenceDialogFragment
            implements SeekBar.OnSeekBarChangeListener {

        private TextView mCurrentValueTextView;

        private int mMinValue = 0;
        private int mMaxValue = 0;
        private int mCurrentValue = 0;

        public SeekBarPreferenceDialogFragment() {
        }

        public static SeekBarPreferenceDialogFragment newInstance(String key) {
            SeekBarPreferenceDialogFragment fragment = new SeekBarPreferenceDialogFragment();
            Bundle args = new Bundle(1);
            args.putString(ARG_KEY, key);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        protected void onBindDialogView(View view) {
            SeekBarPreference preference = (SeekBarPreference) getPreference();
            mMinValue = preference.getMinValue();
            mMaxValue = preference.getMaxValue();
            mCurrentValue = preference.getPersistedInt(0);
            D.log(TAG, "[onBindDialogView][seekbar_debug] minValue: %d; maxValue: %d;", mMinValue, mMaxValue);
            ((TextView) view.findViewById(R.id.min_value)).setText(String.valueOf(mMinValue));
            ((TextView) view.findViewById(R.id.max_value)).setText(String.valueOf(mMaxValue));
            SeekBar seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
            seekBar.setMax(mMaxValue - mMinValue);
            seekBar.setProgress(preference.getPersistedInt(0) - mMinValue);
            seekBar.setOnSeekBarChangeListener(this);
            mCurrentValueTextView = (TextView) view.findViewById(R.id.current_value);
            mCurrentValueTextView.setText(String.valueOf(mCurrentValue));
        }

        @Override
        public void onDialogClosed(boolean result) {
            D.log(TAG, "[onDialogClosed] result: %s", result);
            handleDialogClosed(((SeekBarPreference) getPreference()), result);
        }

        private void handleDialogClosed(SeekBarPreference preference, boolean result) {
            if (result) {
                if (preference.shouldPersist()) {
                    preference.persistInt(mCurrentValue);
                }
                preference.notifyChanged();
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mCurrentValue = progress + mMinValue;
            mCurrentValueTextView.setText(String.valueOf(mCurrentValue));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

    }

}
