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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.parceler.Parcels;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ProfileActivity;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.fragment.ProfileFragment;

@Deprecated
public class ContactQueryHandler extends AsyncQueryHandler {

    public static final int ACTION_BLOCK = 1;
    public static final int ACTION_SET_FAVORITE = 2;
    public static final int ACTION_ADD_FROM_LIST = 3;
    public static final int ACTION_ADD_FROM_PROFILE = 4;
    public static final int ACTION_ACCEPT_INVITE_FROM_LIST = 44;
    public static final int ACTION_ACCEPT_INVITE_FROM_PROFILE = 45;
    public static final int ACTION_ADD_TO_WHITE_LIST = 5;
    public static final int ACTION_EDIT = 6;
    public static final int ACTION_SAVE_PHONEBOOK_CONTACT = 7;
    private static final String TAG = "ContactQueryHandler";

    private Context mContext;
    private Contact mContact;
    private ProgressDialog mProgress;
    private boolean mCloseOnComplete;

    public ContactQueryHandler(
            Context context, Contact contact,
            @Nullable ProgressDialog progress, boolean closeOnComplete) {
        super(context.getContentResolver());
        if (closeOnComplete && !(context instanceof Activity)) {
            throw new IllegalArgumentException("Context must be instance of Activity class");
        }
        mContext = context;
        mContact = contact;
        mProgress = progress;
        mCloseOnComplete = closeOnComplete;
    }

    @Override
    protected void onInsertComplete(int token, Object cookie, Uri uri) {
        hideProgress();
        int newId = Integer.valueOf(uri.getLastPathSegment());
        D.log(TAG, "[onInsertComplete][contact_id_debug] newId: %d", newId);
        if (newId > 0) {
            //TODO: if called from fragment -- just reload it with new id, else -- open ProfileActivity
            Intent intent = new Intent(mContext.getApplicationContext(), ProfileActivity.class);
            mContact.id = newId;
            if (token == ACTION_SAVE_PHONEBOOK_CONTACT) {
                mContact.phonebookId = null;
                mContact.nativeId = "save_hack";
                mContact.directory = false;
            }
            if (token == ACTION_ACCEPT_INVITE_FROM_LIST || token == ACTION_ACCEPT_INVITE_FROM_PROFILE) {
                mContact.invite = false;
            }
            D.log(TAG, "[onInsertComplete][contact_id_debug] mContact.id: %d", mContact.id);
            intent.putExtra(ProfileFragment.CONTACT, Parcels.wrap(mContact));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
            mContext.startActivity(intent);
            if (token == ACTION_ADD_FROM_PROFILE || token == ACTION_ACCEPT_INVITE_FROM_PROFILE) {
                ((Activity) mContext).finish();
            }
        } else {
            Toast.makeText(
                    mContext.getApplicationContext(),
                    R.string.contact_save_failed_msg, Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void hideProgress() {
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
    }

    @Override
    protected void onDeleteComplete(int token, Object cookie, int result) {
        hideProgress();
        if (mCloseOnComplete) {
            ((Activity) mContext).finish();
        }
    }

    //TODO: check result; show error toast if needed
    @Override
    protected void onUpdateComplete(int token, Object cookie, int result) {
        hideProgress();
        if (token == ACTION_BLOCK || token == ACTION_ADD_TO_WHITE_LIST) {
            Toast.makeText(
                    mContext.getApplicationContext(), (String) cookie, Toast.LENGTH_SHORT
            ).show();
        } else if (token == ACTION_EDIT) {
            ((Activity) mContext).finish();
        }
    }

}
