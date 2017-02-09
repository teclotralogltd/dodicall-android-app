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

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.preference.CustomSwitchPreference;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CreateTroubleTicketResult;
import ru.uls_global.dodicall.LogScope;

public class SendReportPreferenceFragment extends BasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "SendReportPreferenceFragment";

    private static final int MAY_SEND_NO = 0;
    private static final int MAY_SEND_PERFECT = 1;
    private static final int MAY_SEND_NO_SBJ_NO_MSG = 2;
    private static final int MAY_SEND_NO_SBJ = 3;
    private static final int MAY_SEND_NO_MSG = 4;

    private MenuItem mSendMenuItem;
    private EditTextPreference mMsgPref;
    private EditTextPreference mSbjPref;
    private CustomSwitchPreference mCallsPref;
    private CustomSwitchPreference mHistoryPref;
    private CustomSwitchPreference mQualityPref;
    private CustomSwitchPreference mChatPref;
    private CustomSwitchPreference mBdPref;
    private CustomSwitchPreference mQueriesPref;
    private CustomSwitchPreference mTracePref;

    private static final class SendReportTask
            extends AsyncTask<Void, Void, CreateTroubleTicketResult> {

        private Activity mActivity;
        private ProgressDialog mProgressDialog;
        private String mSubject;
        private String mMessage;
        private LogScope mLogScope;

        private SendReportTask(
                Activity activity, String subject, String message, LogScope logScope) {
            mActivity = activity;
            mSubject = subject;
            mMessage = message;
            mLogScope = logScope;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = Utils.showProgress(mActivity, R.string.pref_debug_send_progress_msg);
        }

        @Override
        protected CreateTroubleTicketResult doInBackground(Void... params) {
            return BusinessLogic.GetInstance().SendTroubleTicket(mSubject, mMessage, mLogScope);
        }

        @Override
        protected void onPostExecute(CreateTroubleTicketResult result) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
            if (result.getSuccess()) {
                Toast.makeText(
                        mActivity.getApplicationContext(),
                        mActivity.getString(
                                R.string.pref_debug_send_report_success_send_msg,
                                result.getIssueId()
                        ),
                        Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                        mActivity.getApplicationContext(),
                        mActivity.getString(R.string.pref_debug_send_report_fail_send_msg),
                        Toast.LENGTH_LONG
                ).show();
            }
            mActivity.onBackPressed();
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (TextUtils.equals(preference.getKey(), Preferences.Fields.PREF_DEBUG_SEND_REPORT_MESSAGE) ||
                TextUtils.equals(preference.getKey(), Preferences.Fields.PREF_DEBUG_SEND_REPORT_SUBJECT)) {
            preference.setSummary((String) newValue);
        }
        D.log(TAG, "[onPreferenceChange] maySend: %d", maySend(preference, newValue));
        D.log(TAG, "[onPreferenceChange] newValue: %s", newValue);
        mSendMenuItem.setEnabled(maySend(preference, newValue) > MAY_SEND_NO);
        return true;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String s) {
        addPreferencesFromResource(R.xml.pref_debug_send_report);
        setHasOptionsMenu(true);
        mMsgPref = (EditTextPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_MESSAGE
        );
        mSbjPref = (EditTextPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_SUBJECT
        );
        mCallsPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_CALLS_LOG
        );
        mChatPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_CHATS_LOG
        );
        mBdPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_DB_LOG
        );
        mQualityPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_CALLS_QUALITY_LOG
        );
        mQueriesPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_QUERIES_LOG
        );
        mHistoryPref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_CALLS_HISTORY_LOG
        );
        mTracePref = (CustomSwitchPreference) findPreference(
                Preferences.Fields.PREF_DEBUG_SEND_REPORT_TRACE_LOG
        );
        mMsgPref.setOnPreferenceChangeListener(this);
        mSbjPref.setOnPreferenceChangeListener(this);
        mCallsPref.setOnPreferenceChangeListener(this);
        mChatPref.setOnPreferenceChangeListener(this);
        mBdPref.setOnPreferenceChangeListener(this);
        mQualityPref.setOnPreferenceChangeListener(this);
        mQueriesPref.setOnPreferenceChangeListener(this);
        mHistoryPref.setOnPreferenceChangeListener(this);
        mTracePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.send_report) {
            sendReport();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.pref_debug_send_report, menu);
        mSendMenuItem = menu.findItem(R.id.send_report);
    }

    private void showConfirm(
            @StringRes int alertMsg, final String subject,
            final String message, final LogScope logScope) {
        Utils.showConfirm(
                getActivity(),
                alertMsg,
                (dialog, which) -> new SendReportTask(
                        getActivity(), subject, message, logScope
                ).execute()
        );
    }

    private void sendReport() {
        D.log(TAG, "[sendReport]");
        final LogScope logScope = new LogScope();
        final String subject = mSbjPref.getText();
        final String message = mMsgPref.getText();
        boolean callsLog = mCallsPref.isChecked();
        boolean historyLog = mHistoryPref.isChecked();
        boolean qualityLog = mQualityPref.isChecked();
        boolean chatsLog = mChatPref.isChecked();
        boolean dbLog = mBdPref.isChecked();
        boolean queriesLog = mQueriesPref.isChecked();
        boolean traceLog = mTracePref.isChecked();
        logScope.setVoipLog(callsLog);
        logScope.setCallHistoryLog(historyLog);
        logScope.setCallQualityLog(qualityLog);
        logScope.setChatLog(chatsLog);
        logScope.setDatabaseLog(dbLog);
        logScope.setRequestsLog(queriesLog);
        logScope.setTraceLog(traceLog);
        switch (maySend(
                subject, message, callsLog,
                historyLog, qualityLog,
                chatsLog, dbLog, queriesLog,
                traceLog)) {
            case MAY_SEND_PERFECT:
                new SendReportTask(getActivity(), subject, message, logScope).execute();
                break;
            case MAY_SEND_NO_MSG:
                showConfirm(
                        R.string.send_report_confirm_no_msg, subject, message, logScope
                );
                break;
            case MAY_SEND_NO_SBJ:
                showConfirm(
                        R.string.send_report_confirm_no_sbj, subject, message, logScope
                );
                break;
            case MAY_SEND_NO_SBJ_NO_MSG:
                showConfirm(
                        R.string.send_report_confirm_no_msg_no_sbj, subject, message, logScope
                );
                break;
            case MAY_SEND_NO:
                Toast.makeText(
                        getActivity().getApplicationContext(),
                        "Something goes wrong!",
                        Toast.LENGTH_SHORT
                ).show();
                break;
        }
    }

    private int maySend(Preference preference, Object value) {
        String subject =
                preference == mSbjPref ? String.valueOf(value) : mSbjPref.getText();
        String message =
                preference == mMsgPref ? String.valueOf(value) : mMsgPref.getText();
        boolean callsLog =
                preference == mCallsPref ? (boolean) value : mCallsPref.isChecked();
        boolean history =
                preference == mHistoryPref ? (boolean) value : mHistoryPref.isChecked();
        boolean quality =
                preference == mQualityPref ? (boolean) value : mQualityPref.isChecked();
        boolean chatsLog =
                preference == mChatPref ? (boolean) value : mChatPref.isChecked();
        boolean dbLog =
                preference == mBdPref ? (boolean) value : mBdPref.isChecked();
        boolean queriesLog =
                preference == mQueriesPref ? (boolean) value : mQueriesPref.isChecked();
        boolean traceLog =
                preference == mTracePref ? (boolean) value : mTracePref.isChecked();
        return maySend(
                subject, message, callsLog,
                history, quality, chatsLog,
                dbLog, queriesLog, traceLog
        );
    }

    private int maySend(
            String sbj, String msg, boolean calls,
            boolean history, boolean quality, boolean chats,
            boolean db, boolean queries, boolean trace) {
        if (!TextUtils.isEmpty(sbj) && !TextUtils.isEmpty(msg)/* && (calls || chats || db || queries)*/) {
            return MAY_SEND_PERFECT;
        } else if (TextUtils.isEmpty(sbj) && TextUtils.isEmpty(msg) &&
                (calls || history || quality || chats || db || queries || trace)) {
            return MAY_SEND_NO_SBJ_NO_MSG;
        } else if (!TextUtils.isEmpty(sbj) && TextUtils.isEmpty(msg) &&
                (calls || history || quality || chats || db || queries || trace)) {
            return MAY_SEND_NO_MSG;
        } else if (TextUtils.isEmpty(sbj) && !TextUtils.isEmpty(msg) &&
                (calls || history || quality || chats || db || queries || trace)) {
            return MAY_SEND_NO_SBJ;
        } else if (TextUtils.isEmpty(sbj) && TextUtils.isEmpty(msg) &&
                (!calls || !history || !quality || !chats || !db || !queries || !trace)) {
            return MAY_SEND_NO;
        }
        return MAY_SEND_NO;
    }

}
