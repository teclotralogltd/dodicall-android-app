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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import java.util.Arrays;

import ru.swisstok.dodicall.DodicallApplication;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sendBroadcast(new Intent(MainReceiver.MANUAL_NETWORK_CHANGE));

        if (DodicallApplication.isRun() || (isAutoLoggedIn())) {
            startActivity(MainActivity.class);
        } else {
            startActivity(LoginActivity.class);
        }
    }

    private boolean isAutoLoggedIn() {
        boolean result = false;
        try {
            char[] password = StorageUtils.getPassword(this);
            char[] userKey = StorageUtils.getChatKey(this, BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
            result = BL.tryAutoLogin(password, userKey);
            Arrays.fill(password, '1');
            Arrays.fill(userKey, '2');
        } catch (Exception e) {
            D.log(TAG, "Retrieve password error", e);
        }
        return result;
    }

    private void startActivity(Class<?> clazz) {
        Intent intent = new Intent(getApplicationContext(), clazz);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NO_ANIMATION |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }
}


