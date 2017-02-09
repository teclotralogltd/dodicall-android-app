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

package ru.swisstok.dodicall.manager.impl;

import android.content.Context;
import android.content.Intent;

import java.io.Serializable;
import java.lang.ref.WeakReference;

import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.util.LocalBroadcast;

public abstract class BaseManagerImpl implements BaseManager {

    private WeakReference<Context> mContextWeakReference;

    @Override
    public void init(Context context) {
        mContextWeakReference = new WeakReference<>(context);
    }

    protected Context getContext() {
        if (mContextWeakReference != null) {
            Context context = mContextWeakReference.get();
            if (context != null) {
                return context;
            }
        }
        throw new IllegalStateException("Context is Null");
    }

    protected void notifySubscribers(String action, Serializable data) {
        LocalBroadcast.sendBroadcast(mContextWeakReference.get(), new Intent(action).putExtra(EXTRA_DATA, data));
    }

}
