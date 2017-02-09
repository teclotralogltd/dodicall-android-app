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

package ru.swisstok.dodicall.manager.impl;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.DodicallApplication;
import ru.swisstok.dodicall.activity.ActiveCallActivity;
import ru.swisstok.dodicall.activity.IncomingCallActivity;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.manager.BusinessLogicCallback;
import ru.swisstok.dodicall.manager.CallManager;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.NotificationsUtils;

public class CallManagerImpl extends BaseManagerImpl implements CallManager {

    private static final String TAG = "CallManager";

    @Override
    public Call getCurrentCall() {
        final List<Call> calls = BL.getCalls();
        if (!calls.isEmpty()) {
            return calls.get(0);
        }
        return null;
    }

    @Override
    public void clearCache() {
    }

    @Override
    public void onCallback(BusinessLogicCallback.Event event, ArrayList<String> ids) {
        if (event == BusinessLogicCallback.Event.Calls) {
            processCall();
        }
    }

    private void processCall() {
        List<Call> calls = BL.getCalls();
        final AudioManager am = getAudioManager(getContext());
        Context context = getContext();
        if (!calls.isEmpty()) {
            final Call call = calls.get(0);

            if (call.state == Call.STATE_RINGING) {
                if (call.direction == Call.DIRECTION_INCOMING) {
                    D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; ringing; incoming");
                    switch (am.getRingerMode()) {
                        case AudioManager.RINGER_MODE_NORMAL:
                            am.setMode(AudioManager.MODE_RINGTONE);
                            am.setSpeakerphoneOn(true);
                            break;
                        case AudioManager.RINGER_MODE_SILENT:
                            break;
                        case AudioManager.RINGER_MODE_VIBRATE:
                            break;
                    }
                    if (shouldShowIncomingCallScreen(context) && !isCurrentActivity(context, IncomingCallActivity.class)) {
                        D.log(TAG, "[CallbackFunction.run][offline_call_dbg] shouldShowIncomingCall");
                        IncomingCallActivity.start(context, call);
                    }
                    NotificationsUtils.createIncomingCallNotification(context, call, !DodicallApplication.isVisible(context));
                } else if (call.direction == Call.DIRECTION_OUTGOING) {
                    //TODO: remove it
                    D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; ringing; outgoing");
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    am.setSpeakerphoneOn(false);
                    NotificationsUtils.createOutgoingCallNotification(context, call);
                }
            } else if (call.state == Call.STATE_CONVERSATION) {
                D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; conversation;");
                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                if (call.direction == Call.DIRECTION_INCOMING) {
                    am.setSpeakerphoneOn(false);
                }
                CallReceiver.ActiveCallReceiver.activeCall(context);
                if (!isCurrentActivity(context, ActiveCallActivity.class)) {
                    ActiveCallActivity.start(context, call);
                    Preferences.get(context).edit().remove(Preferences.Fields.PREF_DIALPAD_LAST_VAL).apply();
                } else {
                    ActiveCallActivity.updateCall(context, call);
                }
                NotificationsUtils.createActiveCallNotification(context, call);
            } else if (call.state == Call.STATE_DIALING) {
                D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; dialing;");
                if (call.direction == Call.DIRECTION_OUTGOING) {
                    D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls; dialing; outgoing");
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    am.setSpeakerphoneOn(false);
                    NotificationsUtils.createOutgoingCallNotification(context, call);
                }
            }
        } else {
            D.log(TAG, "[CallbackFunction.run][offline_call_dbg] Calls empty; end");
            if (am.isMicrophoneMute()) {
                am.setMicrophoneMute(false);
            }
//                    am.setSpeakerphoneOn(true);
            am.setMode(AudioManager.MODE_NORMAL);
            CallReceiver.EndCallReceiver.endCall(context);
            NotificationsUtils.cancelNotification(context, NotificationsUtils.NotificationType.Call);
        }
//        context.getContentResolver().notifyChange(CURRENT_CALL_URI, null);
    }

    private static AudioManager getAudioManager(Context context) {
        return ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }

    private static boolean isCurrentActivity(Context context, Class<? extends Activity> activityClass) {
        return TextUtils.equals(DodicallApplication.getCurrentActivityName(context), activityClass.getName());
    }

    private static boolean shouldShowIncomingCallScreen(Context context) {
        return (DodicallApplication.isVisible(context) ||
                screenLocked(context) ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
        );
    }

    private static boolean screenLocked(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return !powerManager.isScreenOn();
    }
}
