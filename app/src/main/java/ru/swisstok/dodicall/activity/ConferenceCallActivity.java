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
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ConferenceUsersFragment;
import ru.swisstok.dodicall.fragment.OutgoingConferenceCallFragment;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.Utils;

public class ConferenceCallActivity extends AppCompatActivity implements OutgoingConferenceCallFragment.OnFragmentInteractionListener {

    private static final String TAG = ConferenceCallActivity.class.getSimpleName();

    private PowerManager.WakeLock mProximityWakeLock;

    private boolean mShowed;
    private ConferenceUsersFragment mConferenceUsersFragment;

    @BindView(R.id.list_expland)
    View mListExpandButton;

    @Override
    public void onAttachedToWindow() {
        getWindow().addFlags(getWindowFlags());
        super.onAttachedToWindow();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conference_call);
        setupActionBar();
        ButterKnife.bind(this);

        if (haveProximity()) {
            setupProximity((PowerManager) getSystemService(Context.POWER_SERVICE));
        }

        List<Contact> contactList = ContactsManagerImpl.getInstance().getContacts(ContactsManager.FILTER_ALL, null, false, false);
        ArrayList<Contact> contacts = new ArrayList<>();
        contacts.addAll(contactList);
        contacts.addAll(contactList);
        contacts.addAll(contactList);
        contacts.addAll(contactList);
        contacts.addAll(contactList);
        contacts.addAll(contactList);

        mConferenceUsersFragment = ConferenceUsersFragment.newInstance(contacts);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, OutgoingConferenceCallFragment.newInstance())
                .add(R.id.fragment_container, mConferenceUsersFragment)
                .hide(mConferenceUsersFragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (haveProximity()) {
            final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            //noinspection deprecation
            boolean isWiredHeadsetOn = am.isWiredHeadsetOn();

            D.log(TAG, "[onResume] wired headset is on: %s", isWiredHeadsetOn);
            if (!isWiredHeadsetOn && !am.isBluetoothScoOn()) {
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    protected void setupActionBar() {
        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back_white);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setOnClickListener(v -> {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            if (mShowed) {
                transaction.hide(mConferenceUsersFragment);
            } else {
                transaction.show(mConferenceUsersFragment);
            }

            mShowed = !mShowed;

            mListExpandButton.setSelected(mShowed);

            transaction.commit();
        });
    }

    private void setupProximity(PowerManager pm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                mProximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
            }
        } else {
            mProximityWakeLock = pm.newWakeLock(32, TAG);
        }
    }

    protected int getWindowFlags() {
        return (WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

    protected boolean haveProximity() {
        return true;
    }

    @Override
    public void onAcquireProximity() {
        acquireProximity();
    }

    @Override
    public void onReleaseProximity() {
        releaseProximity();
    }

    @Override
    public void onDecline() {
        finish();
    }

    @Override
    public void onAddUser() {
        Utils.showComingSoon(this);
    }
}
