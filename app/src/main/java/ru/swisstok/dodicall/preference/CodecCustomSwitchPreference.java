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

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CodecConnectionType;
import ru.uls_global.dodicall.CodecSettingModel;
import ru.uls_global.dodicall.CodecSettingsList;
import ru.uls_global.dodicall.CodecType;

public class CodecCustomSwitchPreference extends CustomSwitchPreference {

    private static final String TAG = "CodecCustomSwitchPreference";

    private String mCodecName;
    private CodecType mCodecType;
    private CodecConnectionType mConnectionType;

    public CodecCustomSwitchPreference(Context context, CodecSettingModel codecSettingModel) {
        super(context, null, R.style.AppTheme, R.style.Preference_SwitchPreference_Material);
        mCodecName = codecSettingModel.getName();
        mCodecType = codecSettingModel.getType();
        mConnectionType = codecSettingModel.getConnectionType();
        setKey(getKey());
        setChecked(codecSettingModel.getEnabled());
    }

    @Override
    public String getKey() {
        return mCodecType + "_" + mConnectionType + "_" + mCodecName;
    }

    @Override
    public CharSequence getTitle() {
        return mCodecName;
    }

    private CodecSettingModel getCodec(CodecSettingsList codecList) {
        for (int i = 0; i < codecList.size(); i++) {
            if (checkCodec(codecList.get(i))) {
                return codecList.get(i);
            }
        }
        throw new Preferences.UnsupportedPreferenceKeyException(String.format(
                "unsupported codec key: '%s'", getKey()
        ));
    }

    private boolean checkCodec(CodecSettingModel codec) {
        return (
                codec.getName().equals(mCodecName) &&
                        codec.getType() == mCodecType &&
                        codec.getConnectionType() == mConnectionType
        );
    }

    private boolean getCodecValue(CodecSettingsList codecList) {
        return getCodec(codecList).getEnabled();
    }

    private void setCodecValue(CodecSettingsList codecList, boolean value) {
        getCodec(codecList).setEnabled(value);
        BusinessLogic.GetInstance().ChangeCodecSettings(codecList);
    }

    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        D.log(TAG, "[getPersistedBoolean] key: %s", getKey());
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        return getCodecValue(BusinessLogic.GetInstance().GetDeviceSettings().getCodecSettings());
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        D.log(TAG, "[persistBoolean] newValue: %s; key: %s;", value, getKey());
        if (shouldPersist()) {
            if (value == getPersistedBoolean(!value)) {
                return true;
            }
            setCodecValue(
                    BusinessLogic.GetInstance().GetDeviceSettings().getCodecSettings(), value
            );
            return true;
        }
        return false;
    }
}
