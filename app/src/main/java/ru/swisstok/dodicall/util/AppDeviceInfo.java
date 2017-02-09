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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AppDeviceInfo {

    public static final String PLATFORM_TYPE = "Mobile";
    public static final String PLATFORM_NAME = "Android";
    public static final String DEFAULT_ASSETS_DIRNAME = "appdata";
    private static final String TAG = "AppDeviceInfo";

    public String appName;
    public String appVersion;
    public String appFilesDirName;
    public String appTempDirName;
    public String appAssetsDirName;
    public String deviceId;
    public String deviceModel;
    public String sdkVersion;

    public AppDeviceInfo(Context context) {
        this.appName = getAppName(context);
        this.appVersion = getVersionName(context);
        this.appFilesDirName = context.getFilesDir().getPath();
        this.appTempDirName = context.getCacheDir().getPath();
        this.appAssetsDirName = new File(this.appFilesDirName, DEFAULT_ASSETS_DIRNAME).getPath();
        copyFileOrDir(context.getAssets(), DEFAULT_ASSETS_DIRNAME);
        this.deviceId = getDeviceId(context);
        this.deviceModel = getDeviceModel();
        this.sdkVersion = String.valueOf(Build.VERSION.SDK_INT);
    }

    private void copyFileOrDir(AssetManager am, String path) {
        D.log(TAG, "[copyFileOrDir] path: %s;", path);
        try {
            String[] assets = am.list(path);
            if (assets.length == 0) {
                copyFile(am, path);
            } else {
                File dir = new File(appFilesDirName + "/" + path);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                for (String asset : assets) {
                    copyFileOrDir(am, path + "/" + asset);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyFile(AssetManager am, String filename) {
        try {
            InputStream in = am.open(filename);
            OutputStream out = new FileOutputStream(appFilesDirName + "/" + filename);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    public static String getAppName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0)
            ).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }

    public static String getDeviceModel() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return String.format("%s %s", manufacturer, model);
    }
}
