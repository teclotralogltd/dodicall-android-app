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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.util.SimpleTextWatcher;
import ru.uls_global.dodicall.BusinessLogic;

public class EditMessageActivity extends BaseActivity {

    private static final String EXTRA_CHAT_MESSAGE = "extra_chat_message";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.messageContent)
    EditText mMessageEditView;

    private ChatMessage mMessage;
    private boolean mIsEditDisabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_message);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mMessage = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CHAT_MESSAGE));
        mIsEditDisabled = TextUtils.isEmpty(mMessage.getContent());

        mMessageEditView.setText(mMessage.getContent());
        mMessageEditView.setSelection(mMessage.getContent().length());
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
            correctMessage();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        setTitle(R.string.chat_message_action_edit);
        supportInvalidateOptionsMenu();
    }

    private void correctMessage() {
        String message = mMessageEditView.getText().toString();
        if (!TextUtils.equals(message, mMessage.getContent())) {

            BusinessLogic.GetInstance().CorrectMessage(mMessage.getId(), message);
        }
    }

    public static Intent newIntent(Context context, @NonNull ChatMessage chatMessage) {
        return new Intent(context, EditMessageActivity.class).putExtra(EXTRA_CHAT_MESSAGE, Parcels.wrap(chatMessage));
    }
}
