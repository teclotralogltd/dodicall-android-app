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

package ru.swisstok.dodicall;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;

import io.fabric.sdk.android.Fabric;
import me.leolin.shortcutbadger.ShortcutBadger;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.util.AppDeviceInfo;
import ru.swisstok.dodicall.util.GlobalLifecycleHandler;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;

public class DodicallApplication extends Application {

    @SuppressWarnings("unused")
    private static final String TAG = "DodicallApplication";
    private static boolean run = false;

    private GlobalLifecycleHandler mLifecycleHandler;

    static {
        System.loadLibrary("openssl");
        System.loadLibrary("strophe");
        System.loadLibrary("ffmpeg");
        System.loadLibrary("linphone");
        System.loadLibrary("gpg-error");
        System.loadLibrary("gcrypt");
        System.loadLibrary("dodicall");
    }

    static Context sContext;

    public static class BadgeUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int badgeCount = intent.getIntExtra("count", 0);

            if (badgeCount > 0) {
                ShortcutBadger.applyCount(context, badgeCount);
            } else {
                ShortcutBadger.removeCount(context);
            }
        }
    }

    private BadgeUpdateReceiver mBadgeUpdateReceiver = new BadgeUpdateReceiver();

    @Override
    public void onCreate() {
        super.onCreate();

        ContactsManagerImpl.getInstance().init(getApplicationContext());

        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());
        final AppDeviceInfo info = new AppDeviceInfo(getApplicationContext());
        BusinessLogic.GetInstance().SetupApplicationModel(info.appName, info.appVersion);
        BusinessLogic.GetInstance().SetupDeviceModel(
                info.deviceId, AppDeviceInfo.PLATFORM_TYPE, AppDeviceInfo.PLATFORM_NAME,
                info.deviceModel, info.sdkVersion, info.appAssetsDirName,
                info.appFilesDirName, info.appTempDirName
        );
        BusinessLogic.GetInstance().setupCrashHandlers(info.deviceId, "username", "user@email.com");
        Utils.switchLanguage(getApplicationContext(), Utils.getLocale(getApplicationContext(), true));
        mLifecycleHandler = new GlobalLifecycleHandler();
        registerActivityLifecycleCallbacks(mLifecycleHandler);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        sContext = this;

        LocalBroadcast.registerReceiver(this, mBadgeUpdateReceiver, DataProvider.ACTION_BADGE_UPDATE);
    }

    public static Context getContext() {
        return sContext;
    }

    public boolean isVisible() {
        return mLifecycleHandler != null && mLifecycleHandler.isVisible();
    }

    public String getCurrentActivityName() {
        if (mLifecycleHandler != null) {
            return mLifecycleHandler.getCurrentActivityName();
        }
        return null;
    }

    public static String getCurrentActivityName(Context context) {
        return ((DodicallApplication) context.getApplicationContext()).getCurrentActivityName();
    }

    public static boolean isVisible(Context context) {
        return ((DodicallApplication) context.getApplicationContext()).isVisible();
    }

    public static boolean isRun() {
        return run;
    }

    public static void setRun() {
        run = true;
    }

    public static void clearRun() {
        run = false;
    }

}
