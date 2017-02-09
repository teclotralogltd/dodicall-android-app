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

package ru.swisstok.dodicall.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;

import com.squareup.picasso.Transformation;

import java.lang.ref.WeakReference;
import java.util.List;

import ru.swisstok.dodicall.util.BitmapUtils;

public abstract class BaseAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    public static final int INVALID_POSITION = -1;

    protected static Transformation ROUNDED_TRANSFORMATION = new Transformation() {
        @Override
        public Bitmap transform(Bitmap source) {
            return BitmapUtils.createCircleBitmap(source, true);
        }

        @Override
        public String key() {
            return "rt";
        }
    };

    private WeakReference<Context> mContextWeakReference;
    private List<T> mData;

    public BaseAdapter(Context context, List<T> data) {
        mContextWeakReference = new WeakReference<>(context);
        mData = data;
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
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

    public T getItem(int position) {
        return mData.get(position);
    }

    public void setItem(int position, T data) {
        mData.set(position, data);
    }

    public void updateData(List<T> data) {
        mData = data;
    }

    public List<T> getData() {
        return mData;
    }

    public void clear() {
        mData.clear();
    }
}
