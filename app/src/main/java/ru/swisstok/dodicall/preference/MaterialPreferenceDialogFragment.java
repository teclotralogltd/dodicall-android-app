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

package ru.swisstok.dodicall.preference;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public abstract class MaterialPreferenceDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {

    protected static final String ARG_KEY = "key";

    private DialogPreference mPreference;

    /** Which button was clicked. */
    private int mWhichButtonClicked;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException(
                    "Target fragment must implement TargetFragment interface"
            );
        }
        final DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) rawFragment;
        final String key = getArguments().getString(ARG_KEY);
        mPreference = (DialogPreference) fragment.findPreference(key);
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(mPreference.getDialogTitle())
                .setIcon(mPreference.getDialogIcon())
                .setPositiveButton(mPreference.getPositiveButtonText(), this)
                .setNegativeButton(mPreference.getNegativeButtonText(), this);
        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(mPreference.getDialogMessage());
        }
        onPrepareDialogBuilder(builder);
        // Create the dialog
        final Dialog dialog = builder.create();
        if (needInputMethod()) {
            requestInputMethod(dialog);
        }
        return dialog;
    }

    /**
     * Get the preference that requested this dialog. Available after {@link #onCreate(Bundle)} has
     * been called.
     *
     * @return The {@link DialogPreference} associated with this
     * dialog.
     */
    public DialogPreference getPreference() {
        return mPreference;
    }

    /**
     * Prepares the dialog builder to be shown when the preference is clicked.
     * Use this to set custom properties on the dialog.
     * <p>
     * Do not {@link AlertDialog.Builder#create()} or
     * {@link AlertDialog.Builder#show()}.
     */
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    /**
     * Returns whether the preference needs to display a soft input method when the dialog
     * is displayed. Default is false. Subclasses should override this method if they need
     * the soft input method brought up automatically.
     */
    protected boolean needInputMethod() {
        return false;
    }

    /**
     * Sets the required flags on the dialog window to enable input method window to show up.
     */
    private void requestInputMethod(Dialog dialog) {
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        );
    }

    /**
     * Creates the content view for the dialog (if a custom content view is
     * required). By default, it inflates the dialog layout resource if it is
     * set.
     *
     * @return The content View for the dialog.
     * @see DialogPreference#setLayoutResource(int)
     */
    protected View onCreateDialogView(Context context) {
        final int resId = mPreference.getDialogLayoutResource();
        if (resId == 0) {
            return null;
        }
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(resId, null);
    }

    /**
     * Binds views in the content View of the dialog to data.
     * <p>
     * Make sure to call through to the superclass implementation.
     *
     * @param view The content View of the dialog, if it is custom.
     */
    protected void onBindDialogView(View view) {
        View dialogMessageView = view.findViewById(android.R.id.message);
        if (dialogMessageView != null) {
            final CharSequence message = mPreference.getDialogMessage();
            int newVisibility = View.GONE;
            if (!TextUtils.isEmpty(message)) {
                if (dialogMessageView instanceof TextView) {
                    ((TextView) dialogMessageView).setText(message);
                }
                newVisibility = View.VISIBLE;
            }
            if (dialogMessageView.getVisibility() != newVisibility) {
                dialogMessageView.setVisibility(newVisibility);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    public abstract void onDialogClosed(boolean positiveResult);

}
