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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.task.RenameChatAsyncTask;
import ru.swisstok.dodicall.util.RemoveFormattingTextWatcher;
import ru.swisstok.dodicall.util.Utils;

public class ChatSettingsActivity extends BaseActivity implements RenameChatAsyncTask.OnChatRenamedListener {

    public static final String EXTRA_CHAT = "extra_chat";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.chat_name_edit)
    EditText mChatNameEdit;

    private Chat mChat;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mChat = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_CHAT));

        setupActionBar();

        mChatNameEdit.setEnabled(!mChat.isP2p());
        mChatNameEdit.addTextChangedListener(new RemoveFormattingTextWatcher());

        int max = getResources().getInteger(R.integer.max_chat_name);
        String title = mChat.getTitle();

        if (title.length() > max) {
            title = title.substring(0, max);

            int end = title.lastIndexOf(",");
            if (end <= 0) {
                end = max - 1;
            }

            title = title.substring(0, end) + "…";
        }

        mChatNameEdit.setText(title);
        mChatNameEdit.setSelection(title.length());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_chat_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_ok) {
            save();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void save() {
        if (!mChatNameEdit.getText().toString().equals(mChat.getTitle())) {
            updateChatName();
        } else {
            finish();
        }
    }

    private void updateChatName() {
        showProgress(R.string.progress_message_rename_chat);
        RenameChatAsyncTask.execute(mChat.getId(), mChatNameEdit.getText().toString().trim(), this);
    }

    @Override
    public void onChatRenamed(boolean result, String subject) {
        hideProgress();

        if (!result) {
            Utils.showAlertText(this, R.string.cannot_rename_chat);
        } else {
            finish();
        }
    }

    private void showProgress(@StringRes int textResId) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        mProgressDialog = Utils.showProgress(this, textResId);
    }

    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.back);
        setTitle(R.string.title_activity_chat_settings);
    }

    public static Intent newIntent(Context context, Chat chat) {
        return new Intent(context, ChatSettingsActivity.class)
                .putExtra(EXTRA_CHAT, Parcels.wrap(chat));
    }

}
