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

import java.util.Comparator;

import ru.swisstok.dodicall.api.ChatMessage;

public class ChatMessageComparator implements Comparator<ChatMessage> {
    private static class RownumComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer lhs, Integer rhs) {
            if (lhs < rhs) {
                return -1;
            } else if (lhs > rhs) {
                return 1;
            }
            return 0;
        }
    }

    private static final IncreaseTimeComparator TIME_COMPARATOR = new IncreaseTimeComparator();
    private static final RownumComparator ROWNUM_COMPARATOR = new RownumComparator();

    @Override
    public int compare(ChatMessage lhs, ChatMessage rhs) {
        int r = TIME_COMPARATOR.compare(lhs.getSendTime(), rhs.getSendTime());

        return (r == 0)
                ? ROWNUM_COMPARATOR.compare(lhs.getRownum(), rhs.getRownum())
                : r;
    }
}
