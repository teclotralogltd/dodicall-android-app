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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.LogActivity;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.StringList;

public class LogListFragment extends ListFragment {

    public static final String TAG = "LogListFragment";

    public LogListFragment() {
    }

    public static LogListFragment getInstance(String logType) {
        final Bundle bundle = new Bundle(1);
        bundle.putString(LogActivity.LOG_TYPE, logType);
        final LogListFragment fragment = new LogListFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    //TODO: avoid memory leak
    private class LogTask extends AsyncTask<Void, Void, ArrayAdapter<String>> {

        private String mLogType;

        public LogTask(String logType) {
            D.log(TAG, "[LogTask<init>] logType: %s", logType);
            mLogType = logType;
        }

        @Override
        protected void onPreExecute() {
            setListShown(false);
        }

        @Override
        protected ArrayAdapter<String> doInBackground(Void... params) {
            switch (mLogType) {
                case Preferences.Fields.PREF_DEBUG_CALLS_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetVoipLog());
                case Preferences.Fields.PREF_DEBUG_CALLS_HISTORY:
                    return buildAdapter(BusinessLogic.GetInstance().GetCallHistoryLog());
                case Preferences.Fields.PREF_DEBUG_CALLS_QUALITY_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetCallQualityLog());
                case Preferences.Fields.PREF_DEBUG_CHAT_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetChatLog());
                case Preferences.Fields.PREF_DEBUG_DB_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetDatabaseLog());
                case Preferences.Fields.PREF_DEBUG_QUERIES_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetRequestsLog());
                case Preferences.Fields.PREF_DEBUG_APPLICATION_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetGuiLog());
                case Preferences.Fields.PREF_DEBUG_TRACE_LOG:
                    return buildAdapter(BusinessLogic.GetInstance().GetTraceLog());
            }
            return null;
        }

        private ArrayAdapter<String> buildAdapter(StringList list) {
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(), android.R.layout.simple_list_item_1
            );
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    adapter.add(list.get(i));
                }
            }
            return adapter;
        }

        @Override
        protected void onPostExecute(ArrayAdapter<String> adapter) {
            setListAdapter(adapter);
            getListView().setOnItemLongClickListener((parent, view, position, id) -> {
                Utils.copyTextToClipboard(getContext(), adapter.getItem(position), R.string.clipboard_msg);
                return true;
            });
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty_or_unknown_log));
        new LogTask(getArguments().getString(LogActivity.LOG_TYPE)).execute();
    }

}
