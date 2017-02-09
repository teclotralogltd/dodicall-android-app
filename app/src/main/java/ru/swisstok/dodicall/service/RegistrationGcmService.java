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

package ru.swisstok.dodicall.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.NotificationMode;

public class RegistrationGcmService extends IntentService {

    private static final String TAG = "RegistrationGcmService";

    public RegistrationGcmService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_sender_id), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            D.log(TAG, "GCM Registration Token: " + token);
            sendRegistrationToServer(token);

        } catch (Exception e) {
            D.log(TAG, "Failed to complete token refresh", e);
        }
    }

    private void sendRegistrationToServer(String token) {
        BusinessLogic.GetInstance().RegisterPushTokenOnServer(
                token, NotificationMode.NotificationModeProduction
        );
    }
}