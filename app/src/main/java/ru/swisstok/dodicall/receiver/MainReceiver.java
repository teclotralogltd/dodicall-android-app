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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.IntDef;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.NetworkTechnology;

public class MainReceiver extends BroadcastReceiver {

    public static final String ACTION_MAIN_STATUS_UPDATE = "ru.swisstok.main_status_update";
    public static final String MANUAL_NETWORK_CHANGE = "ru.swisstok.manual_network_change";

    public static final IntentFilter ACTION_FILTER = new IntentFilter();
    private static final String TAG = "MainReceiver";

    static {
        ACTION_FILTER.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        ACTION_FILTER.addAction("android.net.wifi.WIFI_STATE_CHANGED");
//        ACTION_FILTER.addAction(ACTION_MAIN_STATUS_UPDATE);
        ACTION_FILTER.addAction(MANUAL_NETWORK_CHANGE);
    }

    public static final String MAIN_STATUS_BASE = "main_status_base";
    public static final String MAIN_STATUS_EXT = "main_status_ext";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            NETWORK_TYPE_NOT_CONNECTED, NETWORK_TYPE_UNKNOWN,
            NETWORK_TYPE_2G, NETWORK_TYPE_3G,
            NETWORK_TYPE_4G, NETWORK_TYPE_WIFI
    })
    public @interface NetworkType {}
    public static final int NETWORK_TYPE_NOT_CONNECTED = 0;
    public static final int NETWORK_TYPE_UNKNOWN = 1;
    public static final int NETWORK_TYPE_2G = 2;
    public static final int NETWORK_TYPE_3G = 3;
    public static final int NETWORK_TYPE_4G = 4;
    public static final int NETWORK_TYPE_WIFI = 5;

    @NetworkType
    private static int sLastNetworkType = NETWORK_TYPE_UNKNOWN;

    public MainReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals("android.net.conn.CONNECTIVITY_CHANGE", intent.getAction()) ||
                TextUtils.equals("android.net.wifi.WIFI_STATE_CHANGED", intent.getAction()) ||
                TextUtils.equals(MANUAL_NETWORK_CHANGE, intent.getAction())) {
            onNetworkChanged(getNetworkClass(context));
        }
    }

    public void onNetworkChanged(@NetworkType int netWorkType) {
        D.log(TAG, "[onNetworkChanged] technology: %s", getNetworkTechnology(netWorkType));
        if (sLastNetworkType == netWorkType) {
            return;
        }
        sLastNetworkType = netWorkType;
        new Thread(() -> {
            BusinessLogic.GetInstance().SetNetworkTechnology(getNetworkTechnology(netWorkType));
        }).start();
    }

    public static NetworkTechnology getNetworkTechnology(@NetworkType int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_UNKNOWN:
            case NETWORK_TYPE_NOT_CONNECTED:
            default:
                return NetworkTechnology.NetworkTechnologyNone;
            case NETWORK_TYPE_2G:
                return NetworkTechnology.NetworkTechnology2g;
            case NETWORK_TYPE_3G:
                return NetworkTechnology.NetworkTechnology3g;
            case NETWORK_TYPE_4G:
                return NetworkTechnology.NetworkTechnology4g;
            case NETWORK_TYPE_WIFI:
                return NetworkTechnology.NetworkTechnologyWifi;
        }
    }

    @NetworkType
    public static int getNetworkClass(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return NETWORK_TYPE_WIFI;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return getMobileNetworkClass(context);
            }
        } else {
            return NETWORK_TYPE_NOT_CONNECTED;
        }
        return NETWORK_TYPE_UNKNOWN;
    }

    private static int getMobileNetworkClass(Context context) {
        TelephonyManager mTelephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_TYPE_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_TYPE_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return NETWORK_TYPE_4G;
            default:
                return NETWORK_TYPE_UNKNOWN;
        }
    }

}
