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

import org.joda.time.DateTime;

public class DateTimeUtils {
    public static boolean isToday(long millis) {
        DateTime today = currentDateTime();
        DateTime ldt = new DateTime(millis);
        return ldt.getDayOfYear() == today.getDayOfYear();
    }

    public static boolean isYesterday(long millis) {
        DateTime yesterday = currentDateTime().minusDays(1);
        DateTime ldt = new DateTime(millis);
        return ldt.getDayOfYear() == yesterday.getDayOfYear();
    }

    public static boolean isThisYear(long millis) {
        DateTime today = currentDateTime();
        DateTime date = new DateTime(millis);
        return date.getYear() == today.getYear();
    }

    public static DateTime currentDateTime() {
        return new DateTime(System.currentTimeMillis());
    }

    private DateTimeUtils() {

    }
}
