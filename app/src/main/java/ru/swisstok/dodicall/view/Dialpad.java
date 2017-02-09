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

package ru.swisstok.dodicall.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;

import ru.swisstok.dodicall.R;

public class Dialpad extends GridLayout {

    public Dialpad(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public Dialpad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Dialpad(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.dialpad, this);
        setColumnCount(3);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Dialpad, R.attr.dialpadStyle, R.style.Dialpad_Default
        );
        try {
            //divider
            setBackgroundColor(a.getColor(R.styleable.Dialpad_dividerColor, 0));
        } finally {
            a.recycle();
        }
    }

}
