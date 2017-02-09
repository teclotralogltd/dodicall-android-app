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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class IndexerAdapter extends ArrayAdapter<String> {

    public static final String DIVIDER_SYMBOL = "-";

    public IndexerAdapter(Context context, int resource) {
        super(context, resource);
    }

    public IndexerAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public IndexerAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);
    }

    public IndexerAdapter(Context context, int resource, int textViewResourceId, String[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public IndexerAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
    }

    public IndexerAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        view.setClickable(getItem(position).equals(DIVIDER_SYMBOL));
        view.setFocusable(getItem(position).equals(DIVIDER_SYMBOL));
        view.setEnabled(!getItem(position).equals(DIVIDER_SYMBOL));
        return view;
    }
}
