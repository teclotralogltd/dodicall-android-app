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

import java.io.Serializable;

import ru.uls_global.dodicall.BaseUserStatus;

public class ContactStatus implements Serializable {

    public static final int STATUS_OFFLINE = BaseUserStatus.BaseUserStatusOffline.swigValue();
    public static final int STATUS_ONLINE = BaseUserStatus.BaseUserStatusOnline.swigValue();
    public static final int STATUS_AWAY = BaseUserStatus.BaseUserStatusAway.swigValue();
    public static final int STATUS_DND = BaseUserStatus.BaseUserStatusDnd.swigValue();
    public static final int STATUS_HIDDEN = BaseUserStatus.BaseUserStatusHidden.swigValue();

    public static final int STATUS_NOT_FRIEND = 999;

    private String xmppId;
    private int statusId;
    private String extraStatus;

    public ContactStatus() {
    }

    public ContactStatus(String xmppId, int statusId, String extraStatus) {
        this.xmppId = xmppId;
        this.statusId = statusId;
        this.extraStatus = extraStatus;
    }

    public String getXmppId() {
        return xmppId;
    }

    public int getStatusId() {
        return statusId;
    }

    public String getExtraStatus() {
        return extraStatus;
    }
}
