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

package ru.swisstok.dodicall.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import ru.swisstok.dodicall.R;

public abstract class BaseAlertDialog<INPUT, RESULT> extends AlertDialog implements DialogInterface.OnClickListener {
    public interface OnDialogFinishListener<T> {
        void onDialogFinish(T result);
    }

    private OnDialogFinishListener<RESULT> mListener;

    protected BaseAlertDialog(Context context, INPUT oldValue, OnDialogFinishListener<RESULT> listener) {
        super(context, R.style.AppThemeDialog);
        mListener = listener;

        setButton(BUTTON_POSITIVE, getContext().getString(android.R.string.ok), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(android.R.string.cancel), (OnClickListener) null);

        setCancelable(true);
        setView(createContentView(oldValue));
    }

    @Override
    public void onClick(@NonNull DialogInterface dialog, int which) {
        if (mListener != null) {
            mListener.onDialogFinish(getValue());
        }

        dismiss();
    }

    protected abstract View createContentView(INPUT oldValue);

    protected abstract RESULT getValue();
}
