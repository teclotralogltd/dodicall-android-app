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

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import ru.swisstok.dodicall.account.DodicallAccountAuthenticator;

public class AuthenticatorService extends Service {

    private static final Object lock = new Object();
    private DodicallAccountAuthenticator auth;

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (auth == null) {
                auth = new DodicallAccountAuthenticator(this);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DodicallAccountAuthenticator(this).getIBinder();
    }

    public static boolean accountExists(Context context) {
        return (AccountManager.get(context).getAccountsByType(
                DodicallAccountAuthenticator.ACCOUNT_TYPE
        ).length > 0);
    }

}
