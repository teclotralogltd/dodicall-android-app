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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;

import ru.uls_global.dodicall.BusinessLogic;

public class GlobalLifecycleHandler implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "GlobalLifeCycleHandler";
    private int mResumed;
    private int mStopped;
    private boolean mIsVisible;
    private String mCurrentActivity;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mCurrentActivity = activity.getClass().getName();
        if (mResumed++ == mStopped) {
            D.log(TAG, "[onActivityStopped] BL.Resume");
            mIsVisible = true;
            new Thread(() -> BusinessLogic.GetInstance().Resume()).start();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        mCurrentActivity = null;
        if (mResumed == ++mStopped) {
            D.log(TAG, "[onActivityStopped] BL.Pause");
            mIsVisible = false;
            new Thread(() -> BusinessLogic.GetInstance().Pause()).start();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    @Nullable
    public String getCurrentActivityName() {
        return mCurrentActivity;
    }

}
