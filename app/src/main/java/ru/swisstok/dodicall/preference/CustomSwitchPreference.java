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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.util.AttributeSet;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.UserSettingsModel;

public class CustomSwitchPreference extends SwitchPreferenceCompat implements SaveDbPreference {

    private static final String TAG = "CustomSwitchPreference";

    private static final String APP_NAMESPACE = "http://schemas.android.com/apk/res-auto";
    private static final String TABLE_COLUMN = "tableColumn";

    private String mTableColumn;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomSwitchPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CustomSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomSwitchPreference(Context context) {
        super(context);
        init(context, null);
    }

    protected void init(Context context, AttributeSet attrs) {
        setWidgetLayoutResource(android.support.v14.preference.R.layout.preference_widget_switch_compat); //Fix for programmatically added preferences.
        setLayoutResource(android.support.v14.preference.R.layout.preference_material);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.ru_swisstok_preference_SaveDbPreference
            );
            mTableColumn = attrs.getAttributeValue(APP_NAMESPACE, TABLE_COLUMN);
            a.recycle();
        }

        setOnPreferenceChangeListener((preference, newValue) -> {
                    if (TextUtils.equals(getKey(), Preferences.Fields.PREF_DEBUG_MODE) && (boolean) newValue) {
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.pref_debug_mode_title)
                                .setMessage(R.string.pref_debug_mode_on_warning)
                                .setPositiveButton(R.string.turn_on, (dialog, which) -> setChecked(true))
                                .setNegativeButton(R.string.turn_off, (dialog, which) -> setChecked(false))
                                .show();

                        return false;
                    }

                    return true;
                }
        );
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        onSetInitialValue(true, true);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setChecked(getPersistedBoolean(true));
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        D.log(TAG, "[getPersistedBoolean] key: %s", getKey());
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        BusinessLogic logic = BusinessLogic.GetInstance();
        UserSettingsModel settings = logic.GetUserSettings();
        switch (getKey()) {
            case Preferences.Fields.PREF_COMMON_WHITE_LIST:
                return settings.getDoNotDesturbMode();
            case Preferences.Fields.PREF_TELEPHONY_VIDEO_ENABLED:
                return settings.getVideoEnabled();
            case Preferences.Fields.PREF_INTERFACE_ANIMATION:
                return settings.getGuiAnimation();
            case Preferences.Fields.PREF_DEBUG_MODE:
                return settings.getTraceMode();
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        D.log(TAG, "[persistBoolean] newValue: %s; key: %s;", value, getKey());
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                return true;
            }
            BusinessLogic.GetInstance().SaveUserSettings(getTableColumn(), value);
            return true;
        }
        return false;
    }

    @Override
    public String getTableColumn() {
        return mTableColumn;
    }

    @Override
    public void setTableColumn(String tableColumn) {
        mTableColumn = tableColumn;
    }

}
