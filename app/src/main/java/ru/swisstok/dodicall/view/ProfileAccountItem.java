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
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Contact;

public class ProfileAccountItem extends RelativeLayout {

    @SuppressWarnings("unused")
    private static final String TAG = "ProfileAccountItem";

    public static final String TYPE_SIP = "d-sip";
    public static final String TYPE_PHONE = "mobile";

    private String mType;
    private String mNumber;
    private boolean mIsFavorite;
    private Contact mContact;

    protected TextView mTypeTextView;
    protected TextView mNumberTextView;
    protected RadioButton mIsFavoriteRadioButton;
    protected ImageButton mCallButton;

    public ProfileAccountItem(Context context, String type, Contact contact, String number, boolean isFavorite, boolean isPersonal, boolean hideFavorite) {
        super(context);
        mType = type;
        mNumber = number;
        mContact = contact;
        mIsFavorite = isFavorite;
        inflate(context, R.layout.profile_account_item, this);
        mTypeTextView = (TextView) findViewById(R.id.account_type);
        mNumberTextView = (TextView) findViewById(R.id.account_number);
        mIsFavoriteRadioButton = (RadioButton) findViewById(R.id.account_is_favorite);
        mCallButton = (ImageButton) findViewById(R.id.account_call);
        mNumberTextView.setText(mNumber);
        mTypeTextView.setText(mType);
        mIsFavoriteRadioButton.setChecked(mIsFavorite);
        if (hideFavorite) {
            mIsFavoriteRadioButton.setVisibility(GONE);
        }
        if (isPersonal) {
            mIsFavoriteRadioButton.setVisibility(GONE);
            mCallButton.setVisibility(GONE);
        } else {
            mCallButton.setOnClickListener(v -> {
                if (mContact == null) {
                    OutgoingCallActivity.start(getContext(), mNumber);
                } else {
                    OutgoingCallActivity.start(getContext(), mContact, mNumber);
                }
            });
        }
    }

    public void setChecked(boolean checked) {
        mIsFavoriteRadioButton.setChecked(checked);
    }

    public boolean isChecked() {
        return mIsFavoriteRadioButton.isChecked();
    }

    public void setStatus(int status) {
        mCallButton.setImageLevel(StatusesAdapter.getStatusDrawableLevel(status));
    }

    /*@Override
    public void onFinishInflate() {
        D.log(TAG, "[onFinishInflate]");
        super.onFinishInflate();
        mTypeTextView = (TextView) findViewById(R.id.account_type);
        mNumberTextView = (TextView) findViewById(R.id.account_number);
        mIsFavoriteRadioButton = (RadioButton) findViewById(R.id.account_is_favorite);
        mNumberTextView.setText(mNumber);
        mTypeTextView.setText(mType);
        mIsFavoriteRadioButton.setChecked(mIsFavorite);
    }*/
}
