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

package ru.swisstok.dodicall.task;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class ContentResolverAsyncTask<D> extends AsyncTask<Void, Void, D> {
    final Context mContext;

    public ContentResolverAsyncTask(@NonNull Context context) {
        mContext = context;
    }

    @Override
    protected D doInBackground(Void... params) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        if (contentResolver == null) {
            return null;
        }

        return loadInBackground(contentResolver);
    }

    @Nullable
    public abstract D loadInBackground(@NonNull ContentResolver contentResolver);

}
