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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class LongClickableContainerView extends RelativeLayout implements View.OnLongClickListener {

    private boolean mIsDeleting;

    public LongClickableContainerView(Context context) {
        super(context);
    }

    public LongClickableContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongClickableContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupLongClick(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsDeleting || super.onInterceptTouchEvent(ev);
    }

    public void setDeleting(boolean deleting) {
        mIsDeleting = deleting;
    }

    @Override
    public boolean onLongClick(View v) {
        performLongClick();
        return true;
    }

    private void setupLongClick(ViewGroup container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            if (view instanceof ViewGroup) {
                setupLongClick((ViewGroup) view);
            } else {
                view.setOnLongClickListener(this);
            }
        }
    }
}
