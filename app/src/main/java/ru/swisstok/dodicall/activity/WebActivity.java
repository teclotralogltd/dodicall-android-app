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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.D;

public class WebActivity extends BaseActivity {

    private static final String TAG = "WebActivity";
    public static final String URL = "url";
    public static final String TITLE = "title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        String url = getIntent().getStringExtra(URL);
        WebView webView = (WebView) findViewById(R.id.webview);
        if (TextUtils.isEmpty(url) || webView == null) {
            Toast.makeText(
                    getApplicationContext(), "Something went wrong!", Toast.LENGTH_SHORT
            ).show();
            return;
        }
        if (url.endsWith(".pdf")) {
            webView.getSettings().setJavaScriptEnabled(true);
            /*url = String.format(
                    "http://drive.google.com/viewerng/viewer?embedded=true&url=%s", url
            );*/
        }
        webView.loadUrl(url);
        final ActionBar actionBar = getSupportActionBar();
        String title = getIntent().getStringExtra(TITLE);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
            if (TextUtils.isEmpty(title)) {
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView webView, String url) {
                        actionBar.setTitle(webView.getTitle());
                    }

                });
            } else {
                actionBar.setTitle(title);
            }
        }
        D.log(TAG, "[onCreate] title: %s", webView.getTitle());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static void launchWeb(Context context, String url) {
        launchWeb(context, url, null);
    }

    public static void launchWeb(Context context, String url, String title) {
        Intent intent = new Intent(context.getApplicationContext(), WebActivity.class);
        intent.putExtra(WebActivity.URL, url);
        intent.putExtra(WebActivity.TITLE, title);
        context.startActivity(intent);
    }

}
