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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class GcmService extends GcmListenerService {

//    {
//        "alertTitle": "Сергей Васильев",
//        "alertAction": "L",
//        "alertBody": "qqqqqqqqqp",
//        "hasAction": true,
//        "soundName": "m.m4r",
//        "iconBage": 1,
//        "expireInSec": 345600,
//        "dType": "remote",
//        "type": "xmpp",
//        "alertCategory": "XMC",
//        "m": {
//          "f": "00070207533-spb.swisstok.ru@swisstok.ru", f - sipNumber
//          "t": "x", s - sip, x - xmpp
//          "j": "00070207590-spb.swisstok.ru2016061708183748x@conference.swisstok.ru"
//          "c" - имя чата
//          "m" - тип сообщения (0 - текстовое сообщение, 1 - сообщение о смене темы диалога, 2 - аудиосообщение, 3 - нотификация, 4 - контакт, 5 - удаление сообщение из чата)
//                пока будут задействованы только 0, 1 и 3
//          "s" - время отправки сообщения в формате time_t (кол-во секунд, прошедших после 01.01.1970 по Гринвичу)
//          "n" - 0 для p2p диалога, число собеседников в чате для multy-user диалога
//        }
//    }

    private static final String EXTRA_ALERT_TITLE = "alertTitle";
    private static final String EXTRA_ALERT_BODY = "alertBody";
    private static final String EXTRA_JSON_M = "m";
    private static final String EXTRA_JSON_F = "f";
    private static final String EXTRA_JSON_T = "t";
    private static final String SIP_TYPE = "s";

    private static final String TAG = "GcmService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        D.log(TAG, "[onMessageReceived]");
        if (!data.containsKey(EXTRA_ALERT_TITLE) || !data.containsKey(EXTRA_ALERT_BODY) || !data.containsKey(EXTRA_JSON_M)) {
            return;
        }

        String m = data.getString(EXTRA_JSON_M);
        if (m == null || TextUtils.isEmpty(m)) {
            return;
        }

        if (!BusinessLogic.GetInstance().IsLoggedIn()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        JSONObject jsonData = new JSONObject(m);
                        if (jsonData.has(EXTRA_JSON_T)) {
                            String type = jsonData.getString(EXTRA_JSON_T);
                            if (SIP_TYPE.equals(type)) {
                                String sipNumber = jsonData.getString(EXTRA_JSON_F);
                                BL.sendReadyForCall(sipNumber);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "parsing failed", e);
                    }
                    try {
                        char[] password = StorageUtils.getPassword(getApplicationContext());
                        char[] userKey = StorageUtils.getChatKey(getApplicationContext(), BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
                        if (BL.tryAutoLogin(password, userKey)) {
                            D.log(TAG, "[onMessageReceived] success");
                        }
                        Arrays.fill(password, '1');
                        Arrays.fill(userKey, '2');
                    } catch (Exception e) {
                        D.log(TAG, "GCM Login error", e);
                    }
                }
            }.start();
        }
    }
}
