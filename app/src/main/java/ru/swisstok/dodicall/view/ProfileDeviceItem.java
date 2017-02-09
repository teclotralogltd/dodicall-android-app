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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ru.swisstok.dodicall.R;

public class ProfileDeviceItem extends LinearLayout {

    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_NO_CONNECT = 1;
    public static final int STATUS_EDGE = 2;
    public static final int STATUS_3G = 3;
    public static final int STATUS_LTE = 4;
    public static final int STATUS_WIFI = 5;
    public static final int STATUS_ON = 6;
    public static final int STATUS_OFF = 7;

    private String mName;
    private int mStatus;

    private TextView mNameTextView;
    private TextView mStatusTextView;
    private ImageView mMobileStatusImageView;

    public ProfileDeviceItem(Context context, String name, int status) {
        super(context);
        mName = name;
        mStatus = status;
        inflate(context, R.layout.profile_devices_item, this);
        mNameTextView = (TextView) findViewById(R.id.profile_device_name);
        mStatusTextView = (TextView) findViewById(R.id.profile_device_status);
        mMobileStatusImageView = (ImageView) findViewById(R.id.profile_device_mobile_status);
        mNameTextView.setText(name);
        //:(
        if (status >= STATUS_ON) {
            mStatusTextView.setVisibility(View.VISIBLE);
            mMobileStatusImageView.setVisibility(View.GONE);
            if (status == STATUS_ON) {
                mStatusTextView.setText(R.string.profile_device_item_state_on);
                mStatusTextView.setEnabled(true);
                mNameTextView.setEnabled(true);
            } else {
                mStatusTextView.setText(R.string.profile_device_item_state_off);
                mStatusTextView.setEnabled(false);
                mNameTextView.setEnabled(false);
            }
        } else {
            mStatusTextView.setVisibility(View.GONE);
            mMobileStatusImageView.setVisibility(View.VISIBLE);
            mMobileStatusImageView.setImageLevel(status);
        }
    }
}
