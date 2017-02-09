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

package ru.swisstok.dodicall.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

public class LocalBroadcast {
    public static void registerReceiver(Context context, BroadcastReceiver receiver, String... actions) {
        IntentFilter intentFilter = new IntentFilter();
        for (String action : actions) {
            intentFilter.addAction(action);
        }

        registerReceiver(context, receiver, intentFilter);
    }

    public static void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter) {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(receiver);
    }

    public static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
    }

    public static void sendBroadcast(Context context, String action) {
        sendBroadcast(context, new Intent(action));
    }

    private LocalBroadcast() {

    }
}
