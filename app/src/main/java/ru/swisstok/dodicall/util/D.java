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

import android.util.Log;

import ru.swisstok.dodicall.BuildConfig;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.LogLevel;

public class D {
    public static void log(String TAG, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg);
        }
        BusinessLogic.GetInstance().WriteGuiLog(LogLevel.LogLevelDebug, msg);
    }

    public static void log(String TAG, String msgFormat, Object... s) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format(msgFormat, s));
        }
        BusinessLogic.GetInstance().WriteGuiLog(LogLevel.LogLevelDebug, String.format(msgFormat, s));
    }

    public static void log(String TAG, String msg, Exception e) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg, e);
        }
        BusinessLogic.GetInstance().WriteGuiLog(LogLevel.LogLevelError, msg);
    }

    public static void log(String TAG, Exception e, String msgFormat, Object... s) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, String.format(msgFormat, s), e);
        }
        BusinessLogic.GetInstance().WriteGuiLog(LogLevel.LogLevelError, e.getStackTrace().toString());
    }
}
