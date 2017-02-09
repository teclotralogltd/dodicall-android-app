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
import android.support.v7.preference.PreferenceCategory;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.preference.CodecCustomSwitchPreference;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CodecConnectionType;
import ru.uls_global.dodicall.CodecSettingModel;
import ru.uls_global.dodicall.CodecSettingsList;
import ru.uls_global.dodicall.CodecType;

public class CodecsDebugPreferenceFragment extends BasePreferenceFragment {

    private static final Comparator<Pair<CodecType, CodecConnectionType>> CODEC_TYPE_COMPARATOR = (left, right) -> left.first.swigValue() != right.first.swigValue() ? left.first.swigValue() - right.first.swigValue() :
            left.second.swigValue() - right.second.swigValue();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_screen_stub);
        setHasOptionsMenu(true);

        Map<Pair<CodecType, CodecConnectionType>, List<CodecSettingModel>> codecs = new TreeMap<>(CODEC_TYPE_COMPARATOR);

        CodecSettingsList codecSettingsList = BusinessLogic.GetInstance().GetDeviceSettings().getCodecSettings();
        for (int i = 0; i < codecSettingsList.size(); i++) {
            CodecSettingModel codecSetting = codecSettingsList.get(i);
            Pair<CodecType, CodecConnectionType> codecType = Pair.create(codecSetting.getType(), codecSetting.getConnectionType());
            if (!codecs.containsKey(codecType)) {
                codecs.put(codecType, new ArrayList<>());
            }
            codecs.get(codecType).add(codecSetting);
        }

        for (Pair<CodecType, CodecConnectionType> codecType : codecs.keySet()) {
            PreferenceCategory codecsGroup = new PreferenceCategory(getActivity(), null, R.style.AppTheme, R.style.Preference_Category_Material);

            codecsGroup.setTitle(getString(codecType.first == CodecType.CodecTypeAudio ? R.string.pref_debug_codecs_audio_title : R.string.pref_debug_codecs_video_title,
                    getString(codecType.second == CodecConnectionType.ConnectionTypeCell ? R.string.pref_debug_codecs_cell_title : R.string.pref_debug_codecs_wifi_title)));
            getPreferenceScreen().addPreference(codecsGroup);
            for (CodecSettingModel codecSettingModel : codecs.get(codecType)) {
                codecsGroup.addPreference(new CodecCustomSwitchPreference(getActivity(), codecSettingModel));
            }
        }

    }

}
