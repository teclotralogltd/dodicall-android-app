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

package ru.swisstok.dodicall.task;

import android.content.Context;
import android.support.annotation.NonNull;

import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.bl.CallHistoryDetailsList;

public class LoadCallHistoryDetailsTaskLoader extends BaseAsyncTaskLoader<CallHistoryDetailsList> {

    private final CallHistory mCallHistory;

    public LoadCallHistoryDetailsTaskLoader(Context context, @NonNull CallHistory callHistory) {
        super(context);
        mCallHistory = callHistory;
    }

    @Override
    public CallHistoryDetailsList loadInBackground() {
        return BL.getCallHistoryDetailsList(mCallHistory);
    }
}
