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
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.fragment.DodicallSearchFragment;

public class DodicallSearchActivity extends BaseActivity {

    private static final String TAG_SEARCH_FRAGMENT = DodicallSearchFragment.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dodicall_search);
        ButterKnife.bind(this);

        setupActionBar();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, DodicallSearchFragment.getInstance(), TAG_SEARCH_FRAGMENT)
                .commit();
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnTextChanged(R.id.search)
    void performSearch(CharSequence text) {
        startSearch(text);
    }

    @OnEditorAction(R.id.search)
    boolean performSearch(TextView v, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            startSearch(v.getText());
            return true;
        }
        return false;
    }

    private void startSearch(CharSequence s) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_SEARCH_FRAGMENT);
        if (fragment != null && fragment instanceof DodicallSearchFragment) {
            ((DodicallSearchFragment) fragment).startSearch(s.toString());
        }
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, DodicallSearchActivity.class);
    }

    public static void start(@NonNull Context context) {
        context.startActivity(newIntent(context));
    }
}
