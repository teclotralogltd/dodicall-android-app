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

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.D;

public class SyncService extends Service {

    public static final String MODEL_NAME = "model_name";
    public static final String MODEL_CONTACTS = "Contacts";
    public static final String CONTACTS_PRESENCE = "ContactsPresence";
    public static final String CONTACT_SUBSCRIPTIONS = "ContactSubscriptions";
    public static final String OFFLINE = "PresenceOffline";

    private static SyncAdapterImpl sSyncAdapter = null;
    private static final String TAG = "SyncService";
    private static final Object sSyncAdapterLock = new Object();

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        private final ContentResolver mContentResolver;

        public SyncAdapterImpl(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
            this.mContentResolver = context.getContentResolver();
        }

        @Override
        public void onPerformSync(
                Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {
            D.log(TAG, "[performSync]");
            if (TextUtils.equals(extras.getString(MODEL_NAME), MODEL_CONTACTS) ||
                    TextUtils.equals(extras.getString(MODEL_NAME), CONTACTS_PRESENCE)) {
                D.log(TAG, "[performSync] Contacts model");
                mContentResolver.notifyChange(DataProvider.CONTACTS_URI, null);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapterImpl(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }

    public static void requestSyncManually(Account account, String modelName) {
        Bundle extra = new Bundle();
        extra.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extra.putString(MODEL_NAME, modelName);
        ContentResolver.requestSync(account, DataProvider.AUTHORITY, extra);
    }
}
