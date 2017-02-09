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
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;

import ru.swisstok.dodicall.fragment.LogListFragment;

public class LogActivity extends BaseActivity {

    public static final String LOG_TYPE = "log_type";
    public static final String LOG_TITLE = "log_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String title = getIntent().getStringExtra(LOG_TITLE);
        final String type = getIntent().getStringExtra(LOG_TYPE);
        if (!TextUtils.isEmpty(title)) {
            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        getSupportFragmentManager().beginTransaction().add(
                android.R.id.content, LogListFragment.getInstance(type), LogListFragment.TAG
        ).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static void showLog(Context context, String logType, String logTitle) {
        context.startActivity(
                new Intent(context.getApplicationContext(), LogActivity.class)
                        .putExtra(LOG_TYPE, logType)
                        .putExtra(LOG_TITLE, logTitle)
        );
    }
}
