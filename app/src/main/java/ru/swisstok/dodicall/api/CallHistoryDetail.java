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

import org.joda.time.DateTime;
import org.parceler.Parcel;

import ru.uls_global.dodicall.CallDirection;
import ru.uls_global.dodicall.CallEndMode;
import ru.uls_global.dodicall.HistoryAddressType;
import ru.uls_global.dodicall.HistorySourceType;
import ru.uls_global.dodicall.HistoryStatusType;
import ru.uls_global.dodicall.VoipEncryptionType;

@Parcel
public class CallHistoryDetail {
    public static final int DIRECTION_OUTGOING = CallDirection.CallDirectionOutgoing.swigValue();
    public static final int DIRECTION_INCOMING = CallDirection.CallDirectionIncoming.swigValue();

    public static final int ENCRYPTION_NONE = VoipEncryptionType.VoipEncryptionNone.swigValue();
    public static final int ENCRYPTION_SRTP = VoipEncryptionType.VoipEncryptionSrtp.swigValue();

    public static final int END_MODE_NORMAL = CallEndMode.CallEndModeNormal.swigValue();
    public static final int END_MODE_CANCEL = CallEndMode.CallEndModeCancel.swigValue();

    public static final int HISTORY_STATUS_SUCCESS = HistoryStatusType.HistoryStatusSuccess.swigValue();
    public static final int HISTORY_STATUS_ABORTED = HistoryStatusType.HistoryStatusAborted.swigValue();
    public static final int HISTORY_STATUS_MISSED = HistoryStatusType.HistoryStatusMissed.swigValue();
    public static final int HISTORY_STATUS_DECLINED = HistoryStatusType.HistoryStatusDeclined.swigValue();
    public static final int HISTORY_STATUS_ANY = HistoryStatusType.HistoryStatusAny.swigValue();

    public static final int ADDRESS_TYPE_PHONE = HistoryAddressType.HistoryAddressTypePhone.swigValue();
    public static final int ADDRESS_TYPE_SIP11 = HistoryAddressType.HistoryAddressTypeSip11.swigValue();
    public static final int ADDRESS_TYPE_SIP4 = HistoryAddressType.HistoryAddressTypeSip4.swigValue();
    public static final int ADDRESS_TYPE_ANY = HistoryAddressType.HistoryAddressTypeAny.swigValue();

    public static final int HISTORY_SOURCE_PHONEBOOK = HistorySourceType.HistorySourcePhoneBook.swigValue();
    public static final int HISTORY_SOURCE_OTHERS = HistorySourceType.HistorySourceOthers.swigValue();
    public static final int HISTORY_SOURCE_ANY = HistorySourceType.HistorySourceAny.swigValue();

    public int direction;
    public int encryption;
    public int durationInSeconds;
    public int endMode;
    public DateTime startTime;
    public int historyStatus;
    public int addressType;
    public int historySource;
    public String id;
}
