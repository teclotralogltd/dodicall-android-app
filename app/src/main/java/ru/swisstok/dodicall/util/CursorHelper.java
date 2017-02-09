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

import android.database.Cursor;
import android.support.annotation.NonNull;

public class CursorHelper {
    public static String getString(@NonNull Cursor cursor, @NonNull String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public static int getInt(@NonNull Cursor cursor, @NonNull String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    public static long getLong(@NonNull Cursor cursor, @NonNull String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public static boolean getBoolean(@NonNull Cursor cursor, @NonNull String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName)) != 0;
    }

    public static Object getObject(@NonNull Cursor cursor, int columnIndex) {
        int type = cursor.getType(columnIndex);
        switch (type) {
            case Cursor.FIELD_TYPE_BLOB: {
                return cursor.getBlob(columnIndex);
            }
            case Cursor.FIELD_TYPE_FLOAT: {
                return cursor.getFloat(columnIndex);
            }
            case Cursor.FIELD_TYPE_INTEGER: {
                return cursor.getInt(columnIndex);
            }
            case Cursor.FIELD_TYPE_NULL: {
                return null;
            }
            case Cursor.FIELD_TYPE_STRING: {
                return cursor.getString(columnIndex);
            }
        }

        return null;
    }

    private CursorHelper() {
    }
}
