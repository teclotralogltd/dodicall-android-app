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

package ru.swisstok.dodicall.util;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

public class LongClickableMovementMethod extends LinkMovementMethod {

    private GestureDetectorCompat mDetectorCompat;
    private boolean mLongClicked;

    private LongClickableMovementMethod(Context context) {
        mDetectorCompat = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                mLongClicked = true;
                super.onLongPress(e);
            }
        });
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        mDetectorCompat.onTouchEvent(event);
        if (mLongClicked && event.getAction() == MotionEvent.ACTION_UP) {
            Selection.removeSelection(buffer);
            mLongClicked = false;
            return true;
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    public static MovementMethod getInstance(Context context) {
        if (sInstance == null)
            sInstance = new LongClickableMovementMethod(context);

        return sInstance;
    }

    private static LongClickableMovementMethod sInstance;

}
