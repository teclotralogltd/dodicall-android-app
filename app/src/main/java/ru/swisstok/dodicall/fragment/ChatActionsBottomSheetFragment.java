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

package ru.swisstok.dodicall.fragment;

import android.app.Dialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.View;

import ru.swisstok.dodicall.R;

/**
 * @author Roman Radko
 * @since 1.0
 */

public class ChatActionsBottomSheetFragment extends BottomSheetDialogFragment {

    private OnContentSourceActionListener mOnContentSourceActionListener;

    public ChatActionsBottomSheetFragment() {
    }

    public void setOnContentSourceActionListener(OnContentSourceActionListener onContentSourceActionListener) {
        mOnContentSourceActionListener = onContentSourceActionListener;
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.chat_action_selector, null);
        contentView.findViewById(R.id.take_photo_button).setOnClickListener(v -> {
            mOnContentSourceActionListener.onPhoto();
            dismiss();
        });
        contentView.findViewById(R.id.select_from_gallery_button).setOnClickListener(v -> {
            mOnContentSourceActionListener.onGallery();
            dismiss();
        });
        contentView.findViewById(R.id.send_contact_button).setOnClickListener(v -> {
            mOnContentSourceActionListener.onContact();
            dismiss();
        });

        dialog.setContentView(contentView);
    }

    public interface OnContentSourceActionListener {
        void onContact();

        void onGallery();

        void onPhoto();
    }
}
