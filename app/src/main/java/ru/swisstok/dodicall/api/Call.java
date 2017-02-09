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

package ru.swisstok.dodicall.api;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import org.parceler.Parcel;

import ru.uls_global.dodicall.CallDirection;
import ru.uls_global.dodicall.CallModel;
import ru.uls_global.dodicall.CallState;

import static ru.swisstok.dodicall.provider.DataProvider.CurrentCallColumns;

@Parcel
public class Call implements DbStruct {

    public static final int STATE_RINGING = CallState.CallStateRinging.swigValue();
    public static final int STATE_CONVERSATION = CallState.CallStateConversation.swigValue();
    public static final int STATE_DIALING = CallState.CallStateDialing.swigValue();

    public static final int DIRECTION_INCOMING = CallDirection.CallDirectionIncoming.swigValue();
    public static final int DIRECTION_OUTGOING = CallDirection.CallDirectionOutgoing.swigValue();

    public String id;
    public int direction;
    public int encryption;
    public int state;
    public int duration;
    public String identity;
    public int addressType;
    @Nullable
    public Contact contact;

    public Call() {
    }

    public Call(CallModel callModel) {
        id = callModel.getId();
        direction = callModel.getDirection().swigValue();
        encryption = callModel.getEncription().swigValue();
        state = callModel.getState().swigValue();
        duration = callModel.getDuration();
        identity = callModel.getIdentity();
        addressType = callModel.getAddressType().swigValue();
        if (callModel.getContact().isPresent()) {
            contact = new Contact(callModel.getContact().get());
        }
    }

    public Call(Cursor cursor) {
        id = cursor.getString(cursor.getColumnIndex(CurrentCallColumns.ID));
        direction = cursor.getInt(cursor.getColumnIndex(CurrentCallColumns.DIRECTION));
        encryption = cursor.getInt(cursor.getColumnIndex(CurrentCallColumns.ENCRYPTION));
        state = cursor.getInt(cursor.getColumnIndex(CurrentCallColumns.STATE));
        duration = cursor.getInt(cursor.getColumnIndex(CurrentCallColumns.DURATION));
        identity = cursor.getString(cursor.getColumnIndex(CurrentCallColumns.IDENTITY));
        addressType = cursor.getInt(cursor.getColumnIndex(CurrentCallColumns.ADDRESS_TYPE));
        if (cursor.moveToPosition(1)) {
            contact = new Contact(cursor);
        }
    }

    @Override
    public ContentValues toContentValues() {
        return null;
    }

}
