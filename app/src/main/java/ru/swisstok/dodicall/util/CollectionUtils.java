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

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;

public class CollectionUtils {
    public static boolean isEmpty(final Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(final Collection collection) {
        return !isEmpty(collection);
    }

    public interface Joiner<T> {
        CharSequence data(final T value);
    }

    public static <T> String join(@NonNull final Collection<T> collection, @NonNull final Joiner<T> joiner, final char delimeter) {
        StringBuilder sb = new StringBuilder();

        Iterator<T> it = collection.iterator();

        int i = 0;
        while (it.hasNext()) {
            final T value = it.next();
            sb.append(joiner.data(value));

            if (i++ < collection.size() - 1) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    private CollectionUtils() {
    }
}
