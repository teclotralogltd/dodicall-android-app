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
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.fragment.ChangeStatusFragment;
import ru.swisstok.dodicall.util.D;

public class ChangeStatusActivity extends BaseActivity {

    private static final String TAG = "ChangeStatusActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D.log(TAG, "[onCreate]");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            //for locale switch
            actionBar.setTitle(R.string.title_activity_change_status);
        }
        getSupportFragmentManager().beginTransaction().add(
                android.R.id.content, ChangeStatusFragment.getInstance(), ChangeStatusFragment.TAG
        ).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
