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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.uls_global.dodicall.BusinessLogic;

public class DialpadButton extends LinearLayout {

    @BindView(R.id.number)
    TextView mNumber;
    @BindView(R.id.sub_number)
    TextView mSubNumber;

    public DialpadButton(Context context) {
        super(context);
        init(context, null);
    }

    public DialpadButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DialpadButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);
        setFocusable(true);
        final View view = inflate(context, R.layout.view_dialpad_button, this);
        ButterKnife.bind(this, view);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Dialpad,
                R.attr.dialpadStyle, R.style.Dialpad_Default
        );
        try {
            mNumber.setText(a.getString(R.styleable.Dialpad_number));
            mSubNumber.setText(a.getString(R.styleable.Dialpad_sub_number));
            mNumber.setTextColor(a.getColor(R.styleable.Dialpad_textColorNumber, 0));
            mSubNumber.setTextColor(a.getColor(R.styleable.Dialpad_textColorSubNumber, 0));
            setBackgroundResource(a.getResourceId(R.styleable.Dialpad_buttonBackground, 0));
        } finally {
            a.recycle();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!TextUtils.isEmpty(mNumber.getText())) {
            char n = mNumber.getText().charAt(0);

            int action = event.getActionMasked();

            if (action == MotionEvent.ACTION_DOWN) {
                BusinessLogic.GetInstance().PlayDtmf(n);
            } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_OUTSIDE) {
                BusinessLogic.GetInstance().StopDtmf();
            }
        }

        return super.onTouchEvent(event);
    }

    public String getNumber() {
        return mNumber.getText().toString();
    }

}
