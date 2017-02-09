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

package ru.swisstok.dodicall.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.OnClick;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CallOptions;

public class IncomingCallActivity extends CallActivity {

    private static final String TAG = "IncomingCallActivity";

    @BindView(R.id.display_name)
    TextView displayName;

    private Vibrator mVibrator;

    private final CallReceiver.ActiveCallReceiver mActiveCallReceiver =
            new CallReceiver.ActiveCallReceiver() {
                @Override
                public void onActiveCall() {
                    finish();
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcast.registerReceiver(
                this, mActiveCallReceiver, CallReceiver.ActiveCallReceiver.FILTER
        );

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode() != AudioManager.RINGER_MODE_SILENT &&
                mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(new long[]{1000, 1000}, 0);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Call call = Parcels.unwrap(intent.getParcelableExtra(CallReceiver.CALL));

        if (call != null) {
            setCall(call);
            displayName.setText(Utils.getCallIdentity(getCall()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.cancel();
        }
        LocalBroadcast.unregisterReceiver(this, mActiveCallReceiver);
    }

    @Override
    protected void updateCallInfo(Cursor data) {
        if (data != null && data.moveToFirst()) {
            setCall(new Call(data));
            displayName.setText(Utils.getCallIdentity(getCall()));
        }
    }

    @OnClick(R.id.accept)
    void acceptCall() {
        D.log(TAG, "[acceptCall]");
        BusinessLogic.GetInstance().AcceptCall(getCall().id, CallOptions.CallOptionsDefault);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_incoming_call;
    }

    @Override
    protected int getWindowFlags() {
        return (
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
    }

    @Override
    protected boolean haveActionBar() {
        return false;
    }

    @Override
    protected boolean haveProximity() {
        return false;
    }

    public static void start(Context context, Call call) {
        context.sendBroadcast(
                new Intent(CallReceiver.ACTION_INCOMING_CALL).putExtra(
                        CallReceiver.CALL, Parcels.wrap(call)
                )
        );
    }
}
