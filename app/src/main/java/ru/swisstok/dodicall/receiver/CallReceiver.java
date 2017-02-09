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

package ru.swisstok.dodicall.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import ru.swisstok.dodicall.activity.ActiveCallActivity;
import ru.swisstok.dodicall.activity.IncomingCallActivity;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CallOptions;
import ru.uls_global.dodicall.CallsModel;

public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";

    public static final String ACTION_INCOMING_CALL = "ru.swisstok.incoming_call";
    public static final String ACTION_ACTIVE_CALL = "ru.swisstok.active_call";
    public static final String ACTION_HANGUP_CALL = "ru.swisstok.hangup_call";
    public static final String ACTION_ACCEPT_CALL = "ru.swisstok.accept_call";
    public static final String CALL = "Call";

    public static abstract class EndCallReceiver extends BroadcastReceiver {

        public static final String ACTION_END_CALL = "ru.swisstok.end_call";
        public static final IntentFilter FILTER = new IntentFilter(ACTION_END_CALL);

        public EndCallReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_END_CALL)) {
                onEndCall();
            }
        }

        public abstract void onEndCall();

        public static void endCall(Context context) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_END_CALL));
        }
    }

    public static abstract class ActiveCallReceiver extends BroadcastReceiver {

        public static final String ACTION_ACTIVE_CALL = "ru.swisstok.action.ActiveCall";
        public static final IntentFilter FILTER = new IntentFilter(ACTION_ACTIVE_CALL);

        public ActiveCallReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ACTION_ACTIVE_CALL)) {
                onActiveCall();
            }
        }

        public abstract void onActiveCall();

        public static void activeCall(Context context) {
            LocalBroadcast.sendBroadcast(context, new Intent(ACTION_ACTIVE_CALL));
        }

    }

    public CallReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case ACTION_INCOMING_CALL: {
                context.startActivity(
                        new Intent(context.getApplicationContext(), IncomingCallActivity.class)
                                .putExtras(intent.getExtras())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                );
                break;
            }
            case ACTION_ACTIVE_CALL: {
                context.startActivity(new Intent(context.getApplicationContext(), ActiveCallActivity.class)
                        .putExtras(intent.getExtras())
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                );
                break;
            }
            case ACTION_HANGUP_CALL: {
                final CallsModel calls = BusinessLogic.GetInstance().GetAllCalls();
                if (!calls.getSingleCallsList().isEmpty()) {
                    BusinessLogic.GetInstance().HangupCall(
                            calls.getSingleCallsList().get(0).getId()
                    );
                }
                break;
            }
            case ACTION_ACCEPT_CALL: {
                final CallsModel calls = BusinessLogic.GetInstance().GetAllCalls();
                if (!calls.getSingleCallsList().isEmpty()) {
                    BusinessLogic.GetInstance().AcceptCall(
                            calls.getSingleCallsList().get(0).getId(),
                            CallOptions.CallOptionsDefault
                    );
                }
                break;
            }
        }
    }

}
