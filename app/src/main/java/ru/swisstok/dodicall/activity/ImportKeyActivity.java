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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.Utils;

public class ImportKeyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.skip_import_key_btn:
                finish();
                break;
            case R.id.copy_btn:
                startActivity(new Intent(this, EditChatSecurityKeyActivity.class));
                finish();
                break;
            case R.id.qr_code_btn:
                Utils.showComingSoon(this);
                break;
        }
    }
}
