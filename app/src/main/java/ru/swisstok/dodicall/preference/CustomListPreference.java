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
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.SettingsActivity;
import ru.swisstok.dodicall.preference.fragment.BasePreferenceFragment;
import ru.swisstok.dodicall.preference.fragment.InterfacePreferenceFragment;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.EchoCancellationMode;
import ru.uls_global.dodicall.UserSettingsModel;
import ru.uls_global.dodicall.VideoSize;
import ru.uls_global.dodicall.VoipEncryptionType;

public class CustomListPreference extends ListPreference implements SaveDbPreference {

    private static final String TAG = "CustomListPreference";

    private static final String APP_NAMESPACE = "http://schemas.android.com/apk/res-auto";
    private static final String TABLE_COLUMN = "tableColumn";

    private String mTableColumn;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomListPreference(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.ru_swisstok_preference_SaveDbPreference
            );
            mTableColumn = attrs.getAttributeValue(APP_NAMESPACE, TABLE_COLUMN);
            a.recycle();
        }
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        D.log(TAG, "[getPersistedString]");
        UserSettingsModel settings = BusinessLogic.GetInstance().GetUserSettings();
        switch (getKey()) {
            case Preferences.Fields.PREF_TELEPHONY_ENCRYPTION:
                return settings.getVoipEncryption().toString();
            case Preferences.Fields.PREF_TELEPHONY_VIDEO_RESOLUTION_WIFI:
                return settings.getVideoSizeWifi().toString();
            case Preferences.Fields.PREF_TELEPHONY_VIDEO_RESOLUTION_CELL:
                return settings.getVideoSizeCell().toString();
            case Preferences.Fields.PREF_TELEPHONY_NOISE_SUPPRESSION:
//                D.log(TAG, "[getPersistedString] NOISE_SUPPRESSION");
//                D.log(TAG, "[getPersistedString] NOISE_SUPPRESSION; getECM: %s", settings.getEchoCancellationMode());
                return settings.getEchoCancellationMode().toString();
            case Preferences.Fields.PREF_INTERFACE_STYLE:
                return settings.getGuiThemeName();
            case Preferences.Fields.PREF_INTERFACE_LANGUAGE:
                String lang = settings.getGuiLanguage();

                if (TextUtils.isEmpty(lang)) {
                    lang = BusinessLogic.GetInstance().GetGlobalApplicationSettings().getDefaultGuiLanguage();
                }

                return TextUtils.isEmpty(lang)
                        ? getContext().getString(R.string.pref_interface_language_en_value)
                        : lang;
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
    }

    @Override
    protected boolean persistString(String value) {
        D.log(TAG, "[persistString] value: %s", value);
        if (TextUtils.equals(value, getPersistedString(null))) {
            return false;
        }
        switch (getKey()) {
            case Preferences.Fields.PREF_TELEPHONY_ENCRYPTION:
                if (VoipEncryptionType.VoipEncryptionNone.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VoipEncryptionType.VoipEncryptionNone
                    );
                } else {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VoipEncryptionType.VoipEncryptionSrtp
                    );
                }
                break;
            case Preferences.Fields.PREF_TELEPHONY_VIDEO_RESOLUTION_WIFI:
                if (VideoSize.VideoSizeQvga.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSizeQvga
                    );
                } else if (VideoSize.VideoSizeVga.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSizeVga
                    );
                } else {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSize720p
                    );
                }
                break;
            case Preferences.Fields.PREF_TELEPHONY_VIDEO_RESOLUTION_CELL:
                if (VideoSize.VideoSizeVga.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSizeVga
                    );
                } else if (VideoSize.VideoSize720p.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSize720p
                    );
                } else {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), VideoSize.VideoSizeQvga
                    );
                }
                break;
            case Preferences.Fields.PREF_TELEPHONY_NOISE_SUPPRESSION:
                if (EchoCancellationMode.EchoCancellationModeSoft.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), EchoCancellationMode.EchoCancellationModeSoft
                    );
                } else if (EchoCancellationMode.EchoCancellationModeHard.toString().equals(value)) {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), EchoCancellationMode.EchoCancellationModeHard
                    );
                } else {
                    BusinessLogic.GetInstance().SaveUserSettings(
                            getTableColumn(), EchoCancellationMode.EchoCancellationModeOff
                    );
                }
                break;
            case Preferences.Fields.PREF_INTERFACE_STYLE:
                BusinessLogic.GetInstance().SaveUserSettings(getTableColumn(), value);
                break;
            case Preferences.Fields.PREF_INTERFACE_LANGUAGE:
//                BusinessLogic.GetInstance().SaveUserSettings(getTableColumn(), value);
                switchLanguage(getContext().getApplicationContext(), value);
                return false;
            default:
                throw new Preferences.UnsupportedPreferenceKeyException(
                        String.format("key '%s' is not supported", getKey())
                );
        }
        return false;
    }

    private static void switchLanguage(Context context, String lang) {
        if (!TextUtils.equals(context.getResources().getConfiguration().locale.toString(), lang)) {
            Utils.switchLanguage(context, lang);
            context.startActivity(
                    new Intent(context, SettingsActivity.class).setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                    ).putExtra(
                            SettingsActivity.INIT_FRAGMENT_CLASS,
                            InterfacePreferenceFragment.class.getName()
                    ).putExtra(
                            BasePreferenceFragment.PREF_TITLE,
                            context.getString(R.string.pref_header_interface)
                    )
            );
        }
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        onSetInitialValue(true, true);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        D.log(TAG, "[onSetInitialValue] key: %s", getKey());
        setValue(getPersistedString(null));
    }

    @Override
    public CharSequence getSummary() {
        return getEntry();
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
