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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Environment;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.ShareCompat;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;

/**
 * @author Roman Radko
 * @since 1.0
 */

public class ChatSecurityBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String FILE_NAME = "Dodicall_key.txt";

    private String mKey;

    public void setupKey(Context context) {
        try {
            char[] chars = StorageUtils.getChatKey(context, BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
            mKey = new String(chars);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.chat_export_key_selector, null);
        contentView.findViewById(R.id.copy_key_btn).setOnClickListener(v -> {
            copyKeyToClipboard(getActivity(), mKey);
            dismiss();
        });
        contentView.findViewById(R.id.save_to_file_btn).setOnClickListener(v -> {
            storeKeyToFile(getActivity(), mKey);
            dismiss();
        });
        contentView.findViewById(R.id.share_key_btn).setOnClickListener(v -> {
            shareKey(getActivity(), mKey);
            dismiss();
        });

        dialog.setContentView(contentView);
    }

    public static void copyKeyToClipboard(Context context, String key) {
        Utils.copyTextToClipboard(context, key, R.string.clipboard_msg);
    }

    public static void shareKey(Activity activity, String key) {
        ShareCompat.IntentBuilder.from(activity).
                setChooserTitle(R.string.clipboard_msg).
                setText(key).
                setType("text/plain").
                startChooser();
    }

    public static void storeKeyToFile(Context context, String key) {
        File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS, FILE_NAME);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(key.getBytes());
            outputStream.close();
            Toast.makeText(context, R.string.key_was_exported, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
