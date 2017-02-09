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

package ru.swisstok.dodicall.view;

import android.content.Context;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

public class ProfileExtraContactItem extends ProfileAccountItem {

    private static final String TAG = "ProfileExtraContactItem";

    public ProfileExtraContactItem(Context context, String type, String number, boolean personal) {
        super(context, type, null, number, false, personal, false);
        mIsFavoriteRadioButton.setVisibility(GONE);
        D.log(TAG, "<init> personal: %s", personal);
        if (!personal) {
            mCallButton.setImageResource(R.drawable.phone_pstn);
            mCallButton.setVisibility(VISIBLE);
        }
    }
}
