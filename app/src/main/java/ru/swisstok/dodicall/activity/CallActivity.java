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

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.reflect.Method;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;
import ru.uls_global.dodicall.BusinessLogic;

public abstract class CallActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "CallActivity";

    @BindView(R.id.contact_avatar)
    RoundedImageView mContactAvatar;

    private PowerManager.WakeLock mProximityWakeLock;
    private Call mCall;

    private final CallReceiver.EndCallReceiver mEndCallReceiver = new CallReceiver.EndCallReceiver() {
        @Override
        public void onEndCall() {
            D.log(TAG, "[onEndCall][offline_call_dbg]");
            finish();
        }
    };

    private final BroadcastReceiver mAvatarCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(BaseManager.AVATAR_LOADED, intent.getAction())) {
                Contact contact = (Contact) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                if (mCall != null && mCall.contact != null && mCall.contact.dodicallId.equals(contact.dodicallId)) {
                    mContactAvatar.setUrl(contact.avatarPath);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupView();

        LocalBroadcast.registerReceiver(this, mEndCallReceiver, CallReceiver.EndCallReceiver.FILTER);
        LocalBroadcast.registerReceiver(this, mAvatarCallReceiver, BaseManager.AVATAR_LOADED);

        getLoaderManager().initLoader(0, null, this);
        if (haveProximity()) {
            setupProximity((PowerManager) getSystemService(Context.POWER_SERVICE));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (haveProximity()) {
            final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            D.log(TAG, "[onResume] wired headset is on: %s", am.isWiredHeadsetOn());

            //noinspection deprecation
            if (!am.isWiredHeadsetOn() && !am.isBluetoothScoOn()) {
                D.log(TAG, "[onResume] no headset connected, acquire proximity");
                acquireProximity();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        D.log(TAG, "[onPause][debug_proximity] haveProximity: %s", haveProximity());
        if (haveProximity()) {
            releaseProximity();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (chatIsPossible(getCall())) {
            getMenuInflater().inflate(R.menu.activity_outgoing_call, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private static boolean chatIsPossible(@Nullable Call call) {
        return (call != null &&
                call.contact != null &&
                call.contact.isDodicall() &&
                call.contact.id > 0 &&
                call.contact.subscriptionState == Contact.SUBSCRIPTION_STATE_BOTH);
    }


    private CreateChatAsyncTask.OnCreateChatListener mCreateChatListener = chat -> {
        Intent i = ChatActivity.newIntent(CallActivity.this, chat);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(i);
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_chat:
                if (mCall != null && mCall.contact != null) {
                    CreateChatAsyncTask.execute(this, mCall.contact, mCreateChatListener);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAttachedToWindow() {
        getWindow().addFlags(getWindowFlags());
        super.onAttachedToWindow();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(this, mEndCallReceiver);
        LocalBroadcast.unregisterReceiver(this, mAvatarCallReceiver);
    }

    @Optional
    @OnClick({
            R.id.answer_msg, R.id.awaiting, R.id.start_video,
            R.id.add_to_conversation
    })
    void notImplemented() {
        Utils.showComingSoon(this);
    }

    @OnClick(R.id.decline)
    public void declineCall() {
        if (mCall != null) {
            D.log(TAG, "[declineCall] call.id: %s", mCall.id);
            BusinessLogic.GetInstance().HangupCall(mCall.id);
        } else {
            D.log(TAG, "[declineCall] call is null");
        }
    }

    @LayoutRes
    protected abstract int getLayout();

    protected abstract int getWindowFlags();

    protected abstract boolean haveActionBar();

    protected abstract boolean haveProximity();

    //    protected abstract void updateCallInfo(Intent intent);
    protected abstract void updateCallInfo(Cursor data);

    @CallSuper
    protected void setupView() {
        setContentView(getLayout());
        ButterKnife.bind(this);
        if (haveActionBar()) {
            setupActionBar();
        }
    }

    protected void setupActionBar() {
        setSupportActionBar(ButterKnife.findById(this, R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_white);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupProximity(PowerManager pm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                mProximityWakeLock = pm.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG
                );
            }
        } else {
            mProximityWakeLock = pm.newWakeLock(32, TAG);
        }
    }

    void releaseProximity() {
        D.log(TAG, "[releaseProximity][debug_proximity] mProximityWakeLock: %s", mProximityWakeLock);
        if (mProximityWakeLock == null) {
            return;
        }
        if (mProximityWakeLock.isHeld()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mProximityWakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
            } else {
//                mProximityWakeLock.release();
                try {
                    Method release = PowerManager.WakeLock.class.getDeclaredMethod("release", int.class);
                    release.invoke(mProximityWakeLock, 1);
                } catch (Exception e) {
                    D.log(TAG, "[releaseProximity][debug_proximity] something went wrong", e);
                    e.printStackTrace();
                }
            }
        }
    }

    void acquireProximity() {
        D.log(TAG, "[acquireProximity][debug_proximity]");
        if (mProximityWakeLock != null && !mProximityWakeLock.isHeld()) {
            mProximityWakeLock.acquire();
        }
    }

    protected Call getCall() {
        return mCall;
    }

    protected void setCall(Call call) {
        mCall = call;
        if (mCall.contact != null) {
            mContactAvatar.setUrl(mCall.contact.avatarPath);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getApplicationContext(), DataProvider.CURRENT_CALL_URI,
                null, null, null, null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        updateCallInfo(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}
