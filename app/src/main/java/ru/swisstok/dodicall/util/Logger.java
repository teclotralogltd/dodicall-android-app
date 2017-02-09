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

import java.util.concurrent.ConcurrentHashMap;

import ru.swisstok.dodicall.BuildConfig;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.LogLevel;

public class Logger {

    private static final String TAG = "DDC_LOGGER";

    private static ConcurrentHashMap<String, Long> mOperations = new ConcurrentHashMap<>();

    public static void onOperationStart(String operationName) {
        log("Operation started:" + operationName);
        mOperations.put(operationName, System.currentTimeMillis());
    }

    public static void onOperationEnd(String operationName) {
        onOperationEnd(operationName, "");
    }

    public static void onOperationEnd(String operationName, int retrievedCount) {
        onOperationEnd(operationName, "retrieved count=" + retrievedCount);
    }

    public static void onOperationEnd(String operationName, String additionalData) {
        Long startTime = mOperations.remove(operationName);
        log(String.format("Operation ended:%s , time:%s ms , additional data:%s", operationName, System.currentTimeMillis() - (startTime != null ? startTime : 0), additionalData));
    }

    private static void log(String logMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, logMessage);
            BusinessLogic.GetInstance().WriteGuiLog(LogLevel.LogLevelDebug, logMessage);
        }
    }
}
