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

package ru.swisstok.dodicall.view;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.IntDef;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

public class SelectAudioSrcButton extends Button implements View.OnClickListener {

    private static final String TAG = "SelectAudioSrcButton";

    private static final IntentFilter sAudioEventFilter = new IntentFilter();

    static {
        sAudioEventFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        sAudioEventFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        sAudioEventFilter.addAction(Intent.ACTION_HEADSET_PLUG);
    }

    private AlertDialog mAlertDialog;
    private ArrayAdapter<AudioSrc> mAudioSrcAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private BroadcastReceiver mAudioDeviceStateReceiver;
    private List<AudioSrc> mDevicesList = new ArrayList<>();
    private ProximityCallback mCallback;
    @AudioSrc.Pos
    private int mCurrentAudioSrc;
    @AudioSrc.Pos
    private int mLastAudioSrc;
    private AudioSrc phoneSrc;
    private AudioSrc headsetSrc;
    private AudioSrc speakerSrc;

    public SelectAudioSrcButton(Context context) {
        super(context);
    }

    public SelectAudioSrcButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectAudioSrcButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        final AudioManager audioManager =
                (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        final int currentAudioSrc = getCurrentAudioSrcPosition(audioManager);
        phoneSrc = new AudioSrc(AudioSrc.POS_EAR, getContext().getString(R.string.audio_src_phone)) {
            @Override
            public void connect() {
                connectEar(audioManager);
            }
        };
        headsetSrc = new AudioSrc(AudioSrc.POS_EAR, getContext().getString(R.string.audio_src_headset)) {
            @Override
            public void connect() {
                connectEar(audioManager);
            }
        };
        speakerSrc = new AudioSrc(AudioSrc.POS_SPEAKER, getContext().getString(R.string.audio_src_speaker)) {
            @Override
            public void connect() {
                connectSpeaker(audioManager);
            }
        };
        buildAudioSrcAdapter(audioManager);
        mAlertDialog = new AlertDialog.Builder(getContext())
                .setSingleChoiceItems(mAudioSrcAdapter, currentAudioSrc, (dialogInterface, i) -> {
                    final ListView listView = ((AlertDialog) dialogInterface).getListView();
                    int checkedPosition = listView.getCheckedItemPosition();
                    if (checkedPosition != mCurrentAudioSrc) {
                        final AudioSrc audioSrc = (AudioSrc) listView.getAdapter().getItem(checkedPosition);
                        audioSrc.connect();
                        D.log(TAG, "audioSrc: %s", audioSrc);
                    }

                    dialogInterface.dismiss();
                })
                .create();
        mLastAudioSrc = currentAudioSrc;
        mCurrentAudioSrc = currentAudioSrc;
        setOnClickListener(this);
    }

    public interface ProximityCallback {
        //acquire proximity
        void proximityOn();

        //release proximity
        void proximityOff();
    }

    public void setCallback(ProximityCallback callback) {
        mCallback = callback;
    }

    public void release() {
        closeBluetoothProxy();
        if (mAudioDeviceStateReceiver != null) {
            getContext().unregisterReceiver(mAudioDeviceStateReceiver);
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        mAlertDialog.show();
    }

    public static abstract class AudioSrc {

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({POS_EAR, POS_SPEAKER, POS_BLUETOOTH})
        public @interface Pos {
        }

        public static final int POS_EAR = 0;
        public static final int POS_SPEAKER = 1;
        public static final int POS_BLUETOOTH = 2;

        @Pos
        public int type;
        public String name;

        public AudioSrc(@Pos int type, String name) {
            this.name = name;
            this.type = type;
        }

        public abstract void connect();

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object object) {
            return (
                    object instanceof AudioSrc &&
                            ((AudioSrc) object).type == type &&
                            TextUtils.equals(((AudioSrc) object).name, name)
            );
        }

    }

    @AudioSrc.Pos
    private static int getCurrentAudioSrcPosition(AudioManager am) {
        if (am.isBluetoothScoOn()) {
            return AudioSrc.POS_BLUETOOTH;
        }
        if (!am.isSpeakerphoneOn()) {
            return AudioSrc.POS_EAR;
        } else {
            return AudioSrc.POS_SPEAKER;
        }
    }

    private static final class AudioSrcAdapter extends ArrayAdapter<AudioSrc> {

        private AudioManager mAudioManager;

        public AudioSrcAdapter(
                Context context, int resource, List<AudioSrc> objects, AudioManager audioManager) {
            super(context, resource, objects);
            mAudioManager = audioManager;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ((ListView) parent).setItemChecked(
                    position, (position == getCurrentAudioSrcPosition(mAudioManager))
            );
            return super.getView(position, convertView, parent);
        }

    }

    private void buildAudioSrcAdapter(AudioManager am) {
        mAudioSrcAdapter = new AudioSrcAdapter(
                getContext(), android.R.layout.select_dialog_singlechoice, mDevicesList, am
        );
        if (am.isWiredHeadsetOn()) {
            mDevicesList.add(AudioSrc.POS_EAR, headsetSrc);
        } else {
            mDevicesList.add(AudioSrc.POS_EAR, phoneSrc);
        }
        mDevicesList.add(AudioSrc.POS_SPEAKER, speakerSrc);

        mAudioDeviceStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED: {
                        mAudioSrcAdapter.notifyDataSetChanged();
                        break;
                    }
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED: {
                        final BluetoothDevice device =
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, 0);
                        D.log(TAG, "[onReceive][bluetooth_debug] CONNECTION_STATE_CHANGED - state: %d; bluetoothScoOn: %s;", state, am.isBluetoothScoOn());
                        switch (state) {
                            case BluetoothProfile.STATE_CONNECTED: {
                                mDevicesList.add(AudioSrc.POS_BLUETOOTH, new AudioSrc(AudioSrc.POS_BLUETOOTH, device.getName()) {
                                    @Override
                                    public void connect() {
                                        connectBluetooth(am);
                                    }
                                });
                                mAudioSrcAdapter.notifyDataSetChanged();
                                connectBluetooth(am);
                                break;
                            }
                            case BluetoothProfile.STATE_DISCONNECTED: {
                                if (mDevicesList.size() > AudioSrc.POS_BLUETOOTH) {
                                    mDevicesList.remove(AudioSrc.POS_BLUETOOTH);
                                    mAudioSrcAdapter.notifyDataSetChanged();
                                }
                                disconnectBluetooth(am);
                                break;
                            }
                        }
                        break;
                    }
                    case Intent.ACTION_HEADSET_PLUG: {
                        final int state = intent.getIntExtra("state", -1);
                        switch (state) {
                            case 0: {
                                //unplugged
                                D.log(TAG, "[onReceive] wired headset disconnected");
                                mDevicesList.set(AudioSrc.POS_EAR, phoneSrc);
                                mAudioSrcAdapter.notifyDataSetChanged();
                                disconnectEar(am);
                                break;
                            }
                            case 1: {
                                //plugged
                                D.log(TAG, "[onReceive] wired headset connected");
                                mDevicesList.set(AudioSrc.POS_EAR, headsetSrc);
                                mAudioSrcAdapter.notifyDataSetChanged();
                                connectEar(am);
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        };
        getContext().registerReceiver(mAudioDeviceStateReceiver, sAudioEventFilter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getProfileProxy(getContext(), new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HEADSET) {
                        D.log(TAG, "[onServiceConnected][bluetooth_debug]");
                        mBluetoothHeadset = (BluetoothHeadset) proxy;
                        final List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                        if (devices.size() > 0) {
                            mDevicesList.add(AudioSrc.POS_BLUETOOTH, new AudioSrc(AudioSrc.POS_BLUETOOTH, devices.get(0).getName()) {
                                @Override
                                public void connect() {
                                    connectBluetooth(am);
                                }
                            });
                            mAudioSrcAdapter.notifyDataSetChanged();
                            connectBluetooth(am);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    if (profile == BluetoothProfile.HEADSET) {
                        D.log(TAG, "[onServiceDisconnected][bluetooth_debug]");
                        mBluetoothHeadset = null;
                    }
                }
            }, BluetoothProfile.HEADSET);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            init();
        }
    }

    private void closeBluetoothProxy() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
        }
    }

    private void switchPos(@AudioSrc.Pos int newPos) {
        D.log(TAG, "[switchPos] newPos: %d; currentPos: %d", newPos, mCurrentAudioSrc);
        if (newPos != mCurrentAudioSrc) {
            D.log(TAG, "[switchPos]");
            mLastAudioSrc = mCurrentAudioSrc;
            mCurrentAudioSrc = newPos;
        } else {
            D.log(TAG, "[switchPos] don't");
        }
    }

    private void connectBluetooth(AudioManager am) {
        D.log(TAG, "[connectBluetooth][bluetooth_debug] (1) bluetoothScoOn: %s", am.isBluetoothScoOn());
        am.setSpeakerphoneOn(false);
        am.setBluetoothScoOn(true);
        am.startBluetoothSco();
        D.log(TAG, "[connectBluetooth][bluetooth_debug] (2) bluetoothScoOn: %s", am.isBluetoothScoOn());
        if (mCallback != null) {
            mCallback.proximityOff();
        }
        switchPos(AudioSrc.POS_BLUETOOTH);
    }

    private void disconnectBluetooth(AudioManager am) {
        D.log(TAG, "[disconnectBluetooth][bluetooth_debug] bluetoothScoOn: %s", am.isBluetoothScoOn());
        am.setBluetoothScoOn(false);
        am.stopBluetoothSco();
        switch (mLastAudioSrc) {
            case AudioSrc.POS_EAR:
                connectEar(am);
                break;
            case AudioSrc.POS_SPEAKER:
                connectSpeaker(am);
                break;
            case AudioSrc.POS_BLUETOOTH:
                connectEar(am);
                break;
        }
    }

    private void connectEar(AudioManager am) {
        am.setSpeakerphoneOn(false);
        am.setBluetoothScoOn(false);
        am.stopBluetoothSco();
        D.log(TAG, "[connectEar][bluetooth_debug] stopBluetoothSco; scoOn: %s", am.isBluetoothScoOn());
        if (mCallback != null) {
            if (am.isWiredHeadsetOn()) {
                mCallback.proximityOff();
            } else {
                mCallback.proximityOn();
            }
        }
        switchPos(AudioSrc.POS_EAR);
        D.log(TAG, "[connectEar] lastPos: %d; currentPos: %d;", mLastAudioSrc, mCurrentAudioSrc);
    }

    private void disconnectEar(AudioManager am) {
        D.log(TAG, "[disconnectEar] lastPos: %d; currentPos: %d;", mLastAudioSrc, mCurrentAudioSrc);
        switch (mLastAudioSrc) {
            case AudioSrc.POS_BLUETOOTH:
                connectBluetooth(am);
                break;
            case AudioSrc.POS_SPEAKER:
                D.log(TAG, "[disconnectEar] connectSpeaker");
                connectSpeaker(am);
                break;
            case AudioSrc.POS_EAR:
                connectEar(am);
                break;
        }
    }

    private void connectSpeaker(AudioManager am) {
        am.setSpeakerphoneOn(true);
        if (am.isBluetoothScoOn()) {
            am.setBluetoothScoOn(false);
            am.stopBluetoothSco();
            D.log(TAG, "[connectSpeaker][bluetooth_debug] stopBluetoothSco");
        }
        if (mCallback != null) {
            mCallback.proximityOff();
        }
        switchPos(AudioSrc.POS_SPEAKER);
        D.log(TAG, "[connectSpeaker] lastPos: %d; currentPos: %d;", mLastAudioSrc, mCurrentAudioSrc);
    }

}
