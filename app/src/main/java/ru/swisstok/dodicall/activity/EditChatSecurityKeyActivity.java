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

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.SimpleTextWatcher;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class EditChatSecurityKeyActivity extends BaseActivity {

    private static final int MIN_KEY_LENGTH = 2048;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.messageContent)
    EditText mMessageEditView;

    private boolean mIsEditDisabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_message);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mMessageEditView.setHint(R.string.import_key_from_clipboard_hint);
        mMessageEditView.setTextSize(BusinessLogic.GetInstance().GetUserSettings().getGuiFontSize());

        mMessageEditView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                super.afterTextChanged(s);
                if (s.length() == 0 && !mIsEditDisabled) {
                    mIsEditDisabled = true;
                    supportInvalidateOptionsMenu();
                } else if (mIsEditDisabled) {
                    mIsEditDisabled = false;
                    supportInvalidateOptionsMenu();
                }
            }
        });

        setupActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_edit, menu);
        MenuItem editItem = menu.findItem(R.id.action_message_edit);
        editItem.setIcon(mIsEditDisabled ? R.drawable.ic_done_disabled : R.drawable.ic_done);
        editItem.setEnabled(!mIsEditDisabled);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            hideKeyboard(mMessageEditView);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_message_edit) {
            hideKeyboard(mMessageEditView);
            exportKey();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        setTitle(R.string.import_key_from_clipboard_title);
        supportInvalidateOptionsMenu();
    }

    private void exportKey() {
        String key = mMessageEditView.getText().toString();
        key = key.replace(" ", "");
        if (TextUtils.isEmpty(key) || key.length() < MIN_KEY_LENGTH) {
            Snackbar snackbar = Snackbar.make(mToolbar, R.string.import_key_from_clipboard_length_error, Snackbar.LENGTH_SHORT);
            snackbar.show();
            return;
        }
        try {
            StorageUtils.storeChatKey(this, key.toCharArray(), BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
