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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.swisstok.dodicall.R;

//TODO: add ViewHolder
public class ToolBarSpinnerAdapter extends ArrayAdapter<ToolBarSpinnerAdapter.BaseItem> {

    @SuppressWarnings("unused")
    private static final String TAG = "ToolBarSpinnerAdapter";

    public static final int FILTER_ALL = 0;
    public static final int FILTER_DDC = 1;
    public static final int FILTER_PHONE = 2;
    public static final int FILTER_SAVED = 3;
    public static final int FILTER_BLOCKED = 4;
    public static final int FILTER_WHITE = 5;

    @LayoutRes
    private int mSpinnerItem;
    @LayoutRes
    private int mDropDownSpinnerItem;

    public ToolBarSpinnerAdapter(Context context, List<BaseItem> items) {
        this(context, R.layout.toolbar_spinner_item_actionbar, R.layout.toolbar_spinner_item_dropdown, items);
    }

    public ToolBarSpinnerAdapter(Context context, @LayoutRes int spinnerLayout, @LayoutRes int dropDownSpinnerLayout, List<BaseItem> items) {
        super(context, spinnerLayout, items);
        mSpinnerItem = spinnerLayout;
        mDropDownSpinnerItem = dropDownSpinnerLayout;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getDropDownView(int position, View view, @NonNull ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("DROPDOWN")) {
            view = LayoutInflater.from(getContext()).inflate(mDropDownSpinnerItem, parent, false);
            view.setTag("DROPDOWN");
        }

        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));
        int icon = getIcon(position);
        if (icon > 0) {
            AppCompatImageView imageView = (AppCompatImageView) view.findViewById(android.R.id.icon);
            imageView.setImageResource(icon);
        }
        return view;
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        if (view == null || !view.getTag().toString().equals("NON_DROPDOWN")) {
            view = LayoutInflater.from(getContext()).inflate(mSpinnerItem, parent, false);
            view.setTag("NON_DROPDOWN");
        }
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setText(getTitle(position));
        int icon = getIcon(position);
        if (icon > 0) {
            ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
            imageView.setImageResource(icon);
        }
        return view;
    }

    private String getTitle(int position) {
        return position >= 0 && position < getCount() ?
                getContext().getString(getItem(position).title) : "";
    }

    private int getIcon(int position) {
        return position >= 0 && position < getCount() ? getItem(position).icon : -1;
    }

    public static class BaseItem {
        public int title;
        public int icon;
        public int filterType;

        public BaseItem(int title, int icon, int filterType) {
            this.title = title;
            this.icon = icon;
            this.filterType = filterType;
        }

        @Override
        public String toString() {
            return String.valueOf(filterType);
        }
    }
}
