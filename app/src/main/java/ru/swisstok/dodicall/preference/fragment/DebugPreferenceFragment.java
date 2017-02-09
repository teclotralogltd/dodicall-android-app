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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.LogActivity;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;

import static ru.swisstok.dodicall.preference.Preferences.Fields;

public class DebugPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceClickListener {

    public static final String PREF_DEBUG_CLEAR_LOGS = "pref_debug_clear_logs";
    //don't delete
        /*public static final String PREF_DEBUG_KILL_JNI = "pref_debug_kill_jni";
        public static final String PREF_DEBUG_KILL_JAVA = "pref_debug_kill_java";

        private static final Preference.OnPreferenceClickListener sKillListener =
                new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (TextUtils.equals(PREF_DEBUG_KILL_JNI, preference.getKey())) {
                    BusinessLogic.GetInstance().KillMe(9);
                } else if (TextUtils.equals(PREF_DEBUG_KILL_JAVA, preference.getKey())) {
                    throw new RuntimeException("killMe9");
                }
                return true;
            }
        };*/

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_debug);
        setHasOptionsMenu(true);
        findPreference(Fields.PREF_DEBUG_CALLS_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_CALLS_HISTORY).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_CALLS_QUALITY_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_CHAT_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_DB_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_QUERIES_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_TRACE_LOG).setOnPreferenceClickListener(this);
        findPreference(Fields.PREF_DEBUG_APPLICATION_LOG).setOnPreferenceClickListener(this);
        findPreference(PREF_DEBUG_CLEAR_LOGS).setOnPreferenceClickListener(this);

        //don't delete
            /*findPreference(PREF_DEBUG_KILL_JNI).setOnPreferenceClickListener(sKillListener);
            findPreference(PREF_DEBUG_KILL_JAVA).setOnPreferenceClickListener(sKillListener);*/
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case PREF_DEBUG_CLEAR_LOGS:
                clearLogs();
                break;
            case Fields.PREF_DEBUG_CALLS_LOG:
            case Fields.PREF_DEBUG_CHAT_LOG:
            case Fields.PREF_DEBUG_DB_LOG:
            case Fields.PREF_DEBUG_QUERIES_LOG:
            case Fields.PREF_DEBUG_TRACE_LOG:
            case Fields.PREF_DEBUG_APPLICATION_LOG:
            case Fields.PREF_DEBUG_CALLS_HISTORY:
            case Fields.PREF_DEBUG_CALLS_QUALITY_LOG:
                LogActivity.showLog(getActivity(), preference.getKey(), preference.getTitle().toString());
                break;
            default:
                Utils.showComingSoon(getActivity());
                break;
        }
        return true;
    }

    private void clearLogs() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                BusinessLogic.GetInstance().ClearLogs();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.pref_debug_clear_logs_done_msg), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

}
