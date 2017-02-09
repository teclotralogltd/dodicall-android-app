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

package ru.swisstok.dodicall.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Toast;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.fragment.ChatSecurityBottomSheetFragment;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class ExportKeyActivity extends BaseActivity {

    private static final int STORAGE_PERMISSION_REQUEST = 1111;

    private String mKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_key);

        try {
            char[] chars = StorageUtils.getChatKey(this, BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
            mKey = new String(chars);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.skip_export_key_btn:
                finish();
                break;
            case R.id.copy_btn:
                ChatSecurityBottomSheetFragment.copyKeyToClipboard(this, mKey);
                finish();
                break;
            case R.id.share_to_external_app_btn:
                ChatSecurityBottomSheetFragment.shareKey(this, mKey);
                finish();
                break;
            case R.id.save_to_file_btn:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            showPermissionsExplanation();
                            return;
                        }
                    }
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
                    return;
                }
                ChatSecurityBottomSheetFragment.storeKeyToFile(this, mKey);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ChatSecurityBottomSheetFragment.storeKeyToFile(this, mKey);
            } else {
                Toast.makeText(getApplicationContext(), R.string.external_storage_permissions_explanation, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPermissionsExplanation() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissions_explanation)
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST)))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> Toast.makeText(getApplicationContext(), R.string.close_explanation, Toast.LENGTH_SHORT).show()))
                .show();
    }
}
