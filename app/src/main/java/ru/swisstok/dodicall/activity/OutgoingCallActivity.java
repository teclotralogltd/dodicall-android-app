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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.Collections;

import butterknife.BindView;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.receiver.CallReceiver;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.SelectAudioSrcButton;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.CallAddressType;
import ru.uls_global.dodicall.CallOptions;
import ru.uls_global.dodicall.ContactModel;

public class OutgoingCallActivity extends CallActivity {

    private static final String EXTRA_CONTACT = "_contact";
    private static final String EXTRA_PHONE = "_phone";
    private static final String TAG = "OutgoingCallActivity";

    @BindView(R.id.start_video)
    Button mStartVideoButton;
    @BindView(R.id.image_call_type)
    ImageView mCallTypeImage;
    @BindView(R.id.audio_src)
    SelectAudioSrcButton mAudioSrcButton;

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
        if (getIntent().hasExtra(EXTRA_CONTACT)) {
            D.log(TAG, "[onCreate][offline_call_dbg] have extra contact");
            startCall(getIntent());
        }
        LocalBroadcast.registerReceiver(
                this, mActiveCallReceiver, CallReceiver.ActiveCallReceiver.FILTER
        );
    }

    @Override
    public void setupView() {
        super.setupView();
        mAudioSrcButton.setCallback(new SelectAudioSrcButton.ProximityCallback() {
            @Override
            public void proximityOn() {
                acquireProximity();
            }

            @Override
            public void proximityOff() {
                releaseProximity();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(this, mActiveCallReceiver);
        mAudioSrcButton.release();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_outgoing_call;
    }

    @Override
    protected int getWindowFlags() {
        return (
                WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
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

    private void showErrorAndExit(@StringRes int errMsg) {
        Toast.makeText(getApplicationContext(), errMsg, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void updateCallInfo(Cursor data) {
        D.log(TAG, "[updateCallInfo][call_contact_name_debug]");
        if (data != null && data.moveToFirst()) {
            setCall(new Call(data));
            D.log(TAG, "[updateCallInfo][call_contact_name_debug] call.contact: %s", getCall().contact);
            setTitle(Utils.getCallIdentity(getCall()));
            final boolean isDodicallAddress =
                    getCall().addressType == CallAddressType.CallAddressDodicall.swigValue();
            mCallTypeImage.setVisibility(isDodicallAddress ? View.VISIBLE : View.GONE);
            if (isDodicallAddress) {
                mStartVideoButton.setEnabled(true);
                mStartVideoButton.getCompoundDrawables()[1].setAlpha(0xff);
            } else {
                mStartVideoButton.setEnabled(false);
                mStartVideoButton.getCompoundDrawables()[1].setAlpha(0x33);
            }
            invalidateOptionsMenu();
        }
    }

    private void startCall(Intent startIntent) {
        D.log(TAG, "[startCall][offline_call_dbg]");
        final Contact contact = Parcels.unwrap(startIntent.getParcelableExtra(EXTRA_CONTACT));

        if (contact.id == -1) {
            D.log(TAG, "[startCall][offline_call_dbg] contact.id == -1");
            if (CollectionUtils.isEmpty(contact.phones)) {
                D.log(TAG, "[startCall][offline_call_dbg] contact has no number");
                showErrorAndExit(R.string.contact_has_no_number);
                return;
            }

            String url = contact.phones.get(0);

            if (!BusinessLogic.GetInstance().StartCallToUrl(url, CallOptions.CallOptionsDefault)) {
                D.log(TAG, "[startCall][offline_call_dbg] unable to start call (to url); phone: %s", url);
                showErrorAndExit(R.string.unable_start_call);
            } else {
                D.log(TAG, "[startCall][offline_call_dbg] successfully call to url");
            }
        } else if (contact.id == 0) {
            D.log(TAG, "[startCall][offline_call_dbg] contact.id == 0");
            if (CollectionUtils.isEmpty(contact.phones) && CollectionUtils.isEmpty(contact.sips)) {
                D.log(TAG, "[startCall][offline_call_dbg] contact has no number");
                showErrorAndExit(R.string.contact_has_no_number);
                return;
            }
            String number = null;
            if (startIntent.hasExtra(EXTRA_PHONE)) {
                number = startIntent.getStringExtra(EXTRA_PHONE);
            } else {
                if (!TextUtils.isEmpty(contact.dodicallId)) {
                    for (String sip : contact.sips) {
                        if (sip.startsWith(BL.FAVORITE_MARKER)) {
                            number = sip;
                            break;
                        }
                    }
                    if (TextUtils.isEmpty(number)) {
                        number = contact.sips.get(0);
                    }
                } else {
                    number = contact.phones.get(0);
                }
            }

            final ContactModel contactModel = BusinessLogic.GetInstance().RetriveContactByNumber(number);
            if (!BusinessLogic.GetInstance().StartCallToContactUrl(contactModel, number, CallOptions.CallOptionsDefault)) {
                D.log(TAG, "[startCall][offline_call_dbg] unable to start call (to url); phone: %s", number);
                showErrorAndExit(R.string.unable_start_call);
            } else {
                D.log(TAG, "[startCall][offline_call_dbg] successfully call to url");
            }
        } else {
            final ContactModel contactModel =
                    BusinessLogic.GetInstance().GetContactByIdFromCache(contact.id);
            if (!BusinessLogic.GetInstance().StartCallToContact(
                    contactModel, CallOptions.CallOptionsDefault)) {
                D.log(TAG, "[startCall][offline_call_dbg] unable to start call (to contact)");
                showErrorAndExit(R.string.unable_start_call);
            } else {
                D.log(TAG, "[startCall][offline_call_dbg] successfully call to contact");
            }
        }
    }

    public static Intent newIntent(@NonNull Context context, @NonNull Contact contact) {
        return new Intent(context.getApplicationContext(), OutgoingCallActivity.class).putExtra(EXTRA_CONTACT, Parcels.wrap(contact));
    }

    public static Intent newIntent(@NonNull Context context, @NonNull String number) {
        final Contact contact = new Contact();
        contact.id = -1;
        contact.phones = Collections.singletonList(number);
        return newIntent(context, contact);
    }

    public static void start(@NonNull Context context, @NonNull Contact contact) {
        if (!BL.hasActiveCall()) {
            context.startActivity(newIntent(context, contact));
        } else {
            showCallErrorToast(context);
        }
    }

    public static void start(@NonNull Context context, @NonNull Contact contact, String number) {
        if (!BL.hasActiveCall()) {
            Intent intent = newIntent(context, contact);
            intent.putExtra(EXTRA_PHONE, number);
            context.startActivity(intent);
        } else {
            showCallErrorToast(context);
        }
    }

    public static void start(@NonNull Context context, @NonNull String url) {
        if (!BL.hasActiveCall()) {
            context.startActivity(newIntent(context, url));
        } else {
            showCallErrorToast(context);
        }
    }

    private static void showCallErrorToast(Context context) {
        Toast.makeText(context, R.string.active_call_is_present_error, Toast.LENGTH_LONG).show();
    }
}
