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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ActiveCallDialpadFragment;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.SelectAudioSrcButton;
import ru.uls_global.dodicall.CallAddressType;
import ru.uls_global.dodicall.VoipEncryptionType;

public class ActiveCallActivity extends CallActivity {

    private static final String TAG = "ActiveCallActivity";

    private static final String TIME_FORMAT = "%02d:%02d";
    private static final String ACTION_UPDATE_ACTIVE_CALL = "action.UpdateActiveCall";

    @BindView(R.id.start_video)
    Button mStartVideoButton;
    @BindView(R.id.mic)
    Button mMicButton;
    @BindView(R.id.call_duration)
    Chronometer mCallDuration;
    @BindView(R.id.encryption)
    ImageView mEncryption;
    @BindView(R.id.call_type)
    ImageView mCallType;
    @BindView(R.id.extra_buttons_wrapper)
    RelativeLayout mExtraButtonsWrapper;
    @BindView(R.id.top_buttons)
    LinearLayout mTopButtons;
    @BindView(R.id.audio_src)
    SelectAudioSrcButton mAudioSrcButton;

    private BroadcastReceiver mActiveCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Call call = Parcels.unwrap(intent.getParcelableExtra(CallReceiver.CALL));
            updateCallInfo(call);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcast.registerReceiver(this, mActiveCallReceiver, ACTION_UPDATE_ACTIVE_CALL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCallDuration.stop();
        mAudioSrcButton.release();
        LocalBroadcast.unregisterReceiver(this, mActiveCallReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Call call = Parcels.unwrap(intent.getParcelableExtra(CallReceiver.CALL));
        if (call != null) {
            updateCallInfo(call);
        }
    }

    @Override
    protected void setupView() {
        super.setupView();
        mCallDuration.setOnChronometerTickListener(chronometer -> {
            long diff = SystemClock.elapsedRealtime() - chronometer.getBase();
            long seconds = diff / 1000;
            long minutes = TimeUnit.SECONDS.toMinutes(seconds);
            seconds = seconds % 60;
            chronometer.setText(String.format(Locale.ENGLISH, TIME_FORMAT, minutes, seconds));
        });
        mCallDuration.setBase(SystemClock.elapsedRealtime());
        mCallDuration.start();
        mAudioSrcButton.setCallback(new SelectAudioSrcButton.ProximityCallback() {
            @Override
            public void proximityOn() {
                D.log(TAG, "[proximityOn][debug_proximity]");
                acquireProximity();
            }

            @Override
            public void proximityOff() {
                D.log(TAG, "[proximityOff][debug_proximity]");
                releaseProximity();
            }
        });

        if (Preferences.get(this).getBoolean(Preferences.Fields.PREF_DIALPAD_OPEN, false)) {
            toggleExtraButtonsVisibility();
            openDialpad();
        }
    }

    @Override
    protected void updateCallInfo(Cursor data) {
        D.log(TAG, "[updateCallInfo <Cursor>]");
        if (data != null && data.moveToFirst()) {
            updateCallInfo(new Call(data));
        }
    }

    protected void updateCallInfo(Call call) {
        D.log(TAG, "[updateCallInfo <Call>]");
        setCall(call);
        updateTitle(getSupportActionBar());
        D.log(TAG, "[updateCallInfo] call duration: %d", getCall().duration);
        mCallDuration.setBase(SystemClock.elapsedRealtime() - (getCall().duration * 1000));
        if (getCall().encryption == VoipEncryptionType.VoipEncryptionNone.swigValue()) {
            mEncryption.setVisibility(View.GONE);
        } else {
            mEncryption.setVisibility(View.VISIBLE);
        }
        if (getCall().addressType == CallAddressType.CallAddressDodicall.swigValue()) {
            mCallType.setVisibility(View.VISIBLE);
        } else {
            mCallType.setVisibility(View.GONE);
        }

        invalidateOptionsMenu();
    }

    private void updateTitle(@Nullable ActionBar actionBar) {
        if (actionBar != null) {
            actionBar.setTitle(Utils.getCallIdentity(getCall()));
        }
    }

    @OnClick(R.id.mic)
    void toggleMic() {
        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am.isMicrophoneMute()) {
            am.setMicrophoneMute(false);
            mMicButton.getCompoundDrawables()[1].setLevel(0);
        } else {
            am.setMicrophoneMute(true);
            mMicButton.getCompoundDrawables()[1].setLevel(1);
        }
    }

    @OnClick(R.id.extra)
    void toggleExtraButtonsVisibility() {
        if (mExtraButtonsWrapper.getVisibility() == View.GONE) {
            mExtraButtonsWrapper.setVisibility(View.VISIBLE);
            mTopButtons.setVisibility(View.GONE);
        } else {
            mExtraButtonsWrapper.setVisibility(View.GONE);
            mTopButtons.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.open_dialpad)
    void openDialpad() {
        D.log(TAG, "[openDialpad]");
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(TAG)
                .add(R.id.content, ActiveCallDialpadFragment.newInstance())
                .commit();

        Preferences.get(this).edit().putBoolean(Preferences.Fields.PREF_DIALPAD_OPEN, true).apply();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            Preferences.get(this).edit().putBoolean(Preferences.Fields.PREF_DIALPAD_OPEN, false).apply();
        }

        super.onBackPressed();
    }

    @OnClick(R.id.transfer)
    void startTransfer() {
        Call call = getCall();

        ArrayList<Contact> callContacts = new ArrayList<>();
        callContacts.add(call.contact);

        startActivity(
                new Intent(getApplicationContext(), SelectContactToTransferActivity.class)
                        .putExtra(SelectContactToTransferActivity.EXTRA_DISABLED_CONTACTS_LIST, Parcels.wrap(callContacts))
                        .putExtra(SelectContactToTransferActivity.CALL_ID, call.id)
        );
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_active_call;
    }

    @Override
    protected int getWindowFlags() {
        return (
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
    }

    @Override
    protected boolean haveActionBar() {
        return true;
    }

    @Override
    protected boolean haveProximity() {
        return true;
    }

    public static void start(Context context, Call call) {
        context.sendBroadcast(new Intent(CallReceiver.ACTION_ACTIVE_CALL).putExtra(CallReceiver.CALL, Parcels.wrap(call)));
    }

    @Override
    public void declineCall() {
        super.declineCall();
        Preferences.get(this).edit().putBoolean(Preferences.Fields.PREF_DIALPAD_OPEN, false).apply();
    }

    public static void updateCall(Context context, Call call) {
        LocalBroadcast.sendBroadcast(context, new Intent(ACTION_UPDATE_ACTIVE_CALL).putExtra(CallReceiver.CALL, Parcels.wrap(call)));
    }
}
