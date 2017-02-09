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

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import ru.swisstok.dodicall.R;

public class EditTextDialog extends BaseAlertDialog<String, String> {

    @BindView(R.id.edit)
    EditText mEdit;

    protected EditTextDialog(Context context, String oldValue, OnDialogFinishListener<String> listener) {
        super(context, oldValue, listener);
    }

    @SuppressLint("InflateParams")
    @Override
    protected View createContentView(String oldValue) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_text, null);
        ButterKnife.bind(this, view);

        mEdit.setText(oldValue);

        return view;
    }

    @OnTextChanged(value = R.id.edit, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void onTextChanged(CharSequence text) {
        Button okButton = getButton(BUTTON_POSITIVE);

        if (okButton != null) {
            okButton.setEnabled(!TextUtils.isEmpty(text));
        }
    }

    @Override
    protected String getValue() {
        return mEdit.getText().toString();
    }

    public static EditTextDialog show(Context context, String dialogTitle, String oldSubject, OnDialogFinishListener<String> listener) {
        EditTextDialog dialog = new EditTextDialog(context, oldSubject, listener);
        dialog.setTitle(dialogTitle);
        dialog.show();
        return dialog;
    }

}
