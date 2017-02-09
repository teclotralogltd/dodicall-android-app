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

package ru.swisstok.dodicall.preference;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

public class PreferenceHeader extends Preference {

    private static final String TAG = "PreferenceHeader";

    public PreferenceHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PreferenceHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreferenceHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceHeader(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder.getAdapterPosition() == 0) {
            holder.itemView.setSelected(true);
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(getContext(), R.color.colorAccent)
            );
        }
    }

    @Override
    protected void performClick(View view) {
        super.performClick(view);
        D.log(TAG, "[performClick] view: %s", view.getBackground());
        final ViewGroup parent = (ViewGroup) view.getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            parent.getChildAt(i).setSelected(false);
            parent.getChildAt(i).setBackgroundResource(R.drawable.primary_light);
        }
        view.setSelected(true);
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

}
