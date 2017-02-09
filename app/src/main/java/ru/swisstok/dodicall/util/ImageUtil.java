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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

public class ImageUtil {

    private static final int BOUNDS = 5;

    public static Bitmap getAvatar(Context context, String url) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), url);
        drawable.setCircular(true);
        Bitmap source = drawable.getBitmap();
        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
        Canvas canvas = new Canvas(output);
        drawable.setAntiAlias(true);
        drawable.setBounds(BOUNDS, BOUNDS, source.getWidth() - BOUNDS, source.getHeight() - BOUNDS);
        drawable.draw(canvas);
        return output;
    }
}
