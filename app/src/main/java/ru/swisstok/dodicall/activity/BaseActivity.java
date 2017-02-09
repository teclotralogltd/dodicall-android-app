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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.NotificationsUtils;

public abstract class BaseActivity extends AppCompatActivity {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent i = new Intent(context, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BL.isLoggedIn()) {
            startActivity(
                    new Intent(getApplicationContext(), LoginActivity.class).setAction(
                            LoginActivity.ACTION_RELOGIN
                    )
            );
        }

        LocalBroadcast.registerReceiver(this, mBroadcastReceiver, DataProvider.ACTION_LOGOUT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(this, mBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupActivityNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeActivityNotifications();
    }

    public void showKeyboard(View view, int flags) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, flags);
    }

    public void showKeyboard(View view) {
        showKeyboard(view, 0);
    }

    public void showKeyboard() {
        showKeyboard(getWindow().getDecorView(), InputMethodManager.SHOW_FORCED);
    }

    public void hideKeyboard(View view, int flags) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), flags);
    }

    public void hideKeyboard(View view) {
        hideKeyboard(view, 0);
    }

    public void hideKeyboard() {
        hideKeyboard(getWindow().getDecorView());
    }

    protected void setupActivityNotifications() {
        NotificationsUtils.NotificationType notificationType = getNotificationType();
        if (notificationType != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DataProvider.CurrentActivityColumns.ACTIVITY_ID, getInterestedIdForNotifications());
            contentValues.put(DataProvider.CurrentActivityColumns.NOTIFICATION_UPDATE_NEEDED, isWithNotificationUpdate());
            new Thread(() -> getContentResolver().update(DataProvider.makeCurrentActivityUri(notificationType), contentValues, null, null)).start();
        }
    }

    protected void removeActivityNotifications() {
        NotificationsUtils.NotificationType notificationType = getNotificationType();
        if (notificationType != null) {
            new Thread(() -> getContentResolver().delete(DataProvider.makeCurrentActivityUri(notificationType), null, null)).start();
        }
    }

    @Nullable
    protected NotificationsUtils.NotificationType getNotificationType() {
        return null;
    }

    protected String getInterestedIdForNotifications() {
        return null;
    }

    protected boolean isWithNotificationUpdate() {
        return false;
    }
}
