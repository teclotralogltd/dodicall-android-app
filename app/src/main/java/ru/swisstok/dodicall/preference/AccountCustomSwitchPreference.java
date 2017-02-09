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
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.UserSettingsModel;

public class AccountCustomSwitchPreference extends CustomSwitchPreference {

    private static final String TAG = "AccountCustomSwitchPreference";

    private String mServer;
    private boolean mIsDefault = false;

    //never use in xml layouts
    public AccountCustomSwitchPreference(Context context, String server, boolean isDefault) {
        super(context, null, R.style.AppTheme, R.style.Preference_SwitchPreference_Material);
        this.mServer = server;
        this.mIsDefault = isDefault;
        D.log(TAG, "<init> server: %s; isDefault: %s", server, isDefault);
        D.log(TAG, "<init> getDefaultVoipServer: %s", BusinessLogic.GetInstance().GetUserSettings().getDefaultVoipServer());
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        D.log(TAG, "[getPersistedBoolean] key: %s", getKey());
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        UserSettingsModel userSettings = BusinessLogic.GetInstance().GetUserSettings();
        switch (getKey()) {
            case Preferences.Fields.PREF_TELEPHONY_ACCOUNT_DEFAULT:
                return isDefaultServer(userSettings);
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
    }

    private boolean isDefaultServer(UserSettingsModel userSettings) {
        D.log(TAG, "[isDefaultServer] userSettings.getDefaultVoipServer: %s", userSettings.getDefaultVoipServer());
        D.log(TAG, "[isDefaultServer] is default: %s", mIsDefault);
        if (TextUtils.isEmpty(userSettings.getDefaultVoipServer())) {
            return mIsDefault;
        }
        return TextUtils.equals(userSettings.getDefaultVoipServer(), mServer);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        onSetInitialValue(true, true);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setChecked(getPersistedBoolean(true));
        setEnabled(!isDefaultServer(BusinessLogic.GetInstance().GetUserSettings()));
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        D.log(TAG, "[persistBoolean] newValue: %s; key: %s;", value, getKey());
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                return true;
            }
            switch (getKey()) {
                case Preferences.Fields.PREF_TELEPHONY_ACCOUNT_DEFAULT:
                    D.log(TAG, "[persistBoolean] server: %s;", mServer);
                    mIsDefault = value;
                    if (value) {
                        BusinessLogic.GetInstance().SaveUserSettings(getTableColumn(), mServer);
                        setEnabled(false);
                    }
                    break;
                default:
                    throw new Preferences.UnsupportedPreferenceKeyException(
                            String.format("key '%s' is not supported", getKey())
                    );
            }
            return true;
        }
        return false;
    }
}
