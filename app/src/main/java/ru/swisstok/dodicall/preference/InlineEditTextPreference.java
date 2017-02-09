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
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.EditText;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

public class InlineEditTextPreference extends EditTextPreference {

    private static final String TAG = "InlineEditTextPreference";
    private EditText mInlineEditText;

    public InlineEditTextPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public InlineEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public InlineEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InlineEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        //TODO: check why setLayoutResource not working
        mInlineEditText = ((EditText) holder.itemView);
        mInlineEditText.setHint(getTitle());
    }

    @Override
    public String getText() {
        return mInlineEditText.getText().toString();
    }

}
