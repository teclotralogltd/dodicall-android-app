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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import ru.swisstok.dodicall.BuildConfig;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.account.DodicallAccountAuthenticator;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.service.AuthenticatorService;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.swisstok.dodicall.util.Utils;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.GlobalApplicationSettingsModel;
import ru.uls_global.dodicall.ServerAreaModelWrapper;
import ru.uls_global.dodicall.ServerAreasList;
import ru.uls_global.dodicall.UserSettingsModel;

public class LoginActivity extends AccountAuthenticatorActivity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener,
        TextView.OnEditorActionListener, ViewTreeObserver.OnGlobalLayoutListener {

    private static final String TAG = "LoginActivity";
    private static final String LAST_LOGIN = "last_login";
    private static final String[] locales = {"ru", "en", "tr"};

    public static final String ACTION_RELOGIN = "action_relogin";

    private static ArrayList<ToolBarSpinnerAdapter.BaseItem> sLangs = new ArrayList<>();

    static {
        sLangs.add(new LangItem(R.string.pref_interface_language_ru, R.drawable.ic_russia, locales[0]));
        sLangs.add(new LangItem(R.string.pref_interface_language_en, R.drawable.ic_gb, locales[1]));
        sLangs.add(new LangItem(R.string.pref_interface_language_tr, R.drawable.ic_turkey, locales[2]));
    }

    private View mContent;
    private ImageView mLogo;
    private EditText mLoginEditText;
    private EditText mPasswordEditText;
    private ToolBarSpinnerAdapter mLangAdapter;
    private TextView mLoginFailure;
    private TextView mVersion;
    private Button mLoginButton;
    private RelativeLayout mKeyboardHackWrapper;
    private Spinner mLangSpinner;

    private int mServerArea = 0;
    private ServerAreasList mServerAreas;

    private int switchServerCounter = 0;
    private boolean previousKeyboardHiddenState;

    private static class LangItem extends ToolBarSpinnerAdapter.BaseItem {

        private String locale;

        private LangItem(int title, int icon, String locale) {
            super(title, icon, -1);
            this.locale = locale;
        }

    }

    private GlobalApplicationSettingsModel mGlobalSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D.log(TAG, "[onCreate]");
        setContentView(R.layout.activity_login);
        mContent = findViewById(android.R.id.content);
        setupUiForKeyboard(mContent);
        setupSpinner();
        mVersion = (TextView) findViewById(R.id.app_version_number);
        mLoginFailure = (TextView) findViewById(R.id.login_failure);
        mLoginEditText = (EditText) findViewById(R.id.login);
        if (BuildConfig.DEBUG) {
            mLoginEditText.setOnKeyListener((v, keyCode, event) -> {
                if (KeyEvent.KEYCODE_CLEAR == keyCode) {
                    mLoginEditText.setText("");
                }
                return false;
            });
        }
        mPasswordEditText = (EditText) findViewById(R.id.password);
        mPasswordEditText.setText("");
        mLoginButton = (Button) findViewById(R.id.login_button);
        mPasswordEditText.setOnEditorActionListener(this);
        mLogo = (ImageView) findViewById(R.id.big_logo);
        mKeyboardHackWrapper = (RelativeLayout) findViewById(R.id.keyboard_hack_wrapper);
        mContent.getViewTreeObserver().addOnGlobalLayoutListener(this);
        FrameLayout progress = (FrameLayout) findViewById(R.id.progress);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mGlobalSettings = BusinessLogic.GetInstance().GetGlobalApplicationSettings();
                mServerAreas = BusinessLogic.GetInstance().RetrieveAreas();
                mServerArea = mGlobalSettings.getArea();
                return null;
            }

            @Override
            public void onPostExecute(Void result) {
                init(savedInstanceState);
                progress.setVisibility(View.GONE);
            }
        }.execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(LAST_LOGIN, mLoginEditText.getText().toString());
        super.onSaveInstanceState(outState);
    }

    private void init(Bundle savedInstanceState) {
        setServerArea(getCurrentServerArea());
//        setVersion(mServerAreas.get(mServerArea));
        setServerArea(getCurrentServerArea());
        TextView registration = (TextView) findViewById(R.id.registration);
        TextView passwordRestore = (TextView) findViewById(R.id.password_restore);
        mLogo.setOnClickListener(this);
        mLoginButton.setOnClickListener(this);
        registration.setOnClickListener(this);
        passwordRestore.setOnClickListener(this);
        if (accountExists()) {
            final String lastLogin = savedInstanceState == null ? null : savedInstanceState.getString(LAST_LOGIN);
            if (!TextUtils.isEmpty(lastLogin)) {
                mLoginEditText.setText(lastLogin);
            } else {
                mLoginEditText.setText(mGlobalSettings.getLastLogin());
            }
            mLoginEditText.setSelection(mLoginEditText.getText().length());
//            if (TextUtils.equals(getIntent().getAction(), ACTION_RELOGIN)) {
//                mPasswordEditText.setText(mGlobalSettings.getLastPassword());
//                login(
//                        mGlobalSettings.getLastLogin(),
//                        mGlobalSettings.getLastPassword()
//                );
//            }
        }
    }

    private void setupSpinner() {
        mLangSpinner = (Spinner) findViewById(R.id.lang);
        mLangAdapter = new ToolBarSpinnerAdapter(this, R.layout.lang_spinner_item, R.layout.lang_spinner_item, sLangs);
        mLangSpinner.setAdapter(mLangAdapter);
        int pos = Arrays.asList(locales).indexOf(Utils.getLocale(getApplicationContext()));
        mLangSpinner.setSelection(pos == -1 ? 0 : pos);
        mLangSpinner.setOnItemSelectedListener(this);
    }

    public void setupUiForKeyboard(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideKeyboard();
                return false;
            });
        }
        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setupUiForKeyboard(((ViewGroup) view).getChildAt(i));
            }
        }
    }

    private void switchLanguage(String lang) {
        if (!Utils.getLocale(getApplicationContext()).equals(lang)) {
            Utils.switchLanguage(getApplicationContext(), lang, true);
            recreate();
        }
    }

    private static class LoginTask extends AsyncTask<Void, Void, BL.LoginResult> {

        private String mUsername;
        private char[] mPassword;

        private LoginActivity mActivity;
        private ProgressDialog mProgressDialog;

        private int mServerArea;

        private LoginTask(LoginActivity activity, String username, String password, int serverArea) {
            mActivity = activity;
            mUsername = username;
            mPassword = password.toCharArray();
            mServerArea = serverArea;
        }

        @Override
        protected void onPreExecute() {
            mProgressDialog = Utils.showProgress(mActivity, R.string.login_progress_dialog_text);
        }

        @Override
        protected BL.LoginResult doInBackground(Void... params) {
            final BusinessLogic logic = BusinessLogic.GetInstance();

            char[] userKey = new char[]{};
            try {
                userKey = StorageUtils.getChatKey(mActivity, mUsername);
            } catch (Exception e) {
                e.printStackTrace();
            }
            BL.LoginResult result = BL.login(mActivity, mUsername, mPassword, userKey, mServerArea);
            Arrays.fill(userKey, '1');

            if (result != null && result.isSuccess()) {
                UserSettingsModel userSettings = logic.GetUserSettings();
                userSettings.setGuiThemeName(mActivity.getString(R.string.pref_interface_style_default_value));
                userSettings.setGuiLanguage(Utils.getLocale(
                        mActivity.getApplicationContext(), true
                ));
                logic.SaveUserSettings(userSettings);
            }

            return result;
        }

        @Override
        protected void onPostExecute(BL.LoginResult result) {
            if (mActivity.afterLogin(result)) {
                try {
                    StorageUtils.storePassword(mActivity, mPassword);
                } catch (Exception e) {
                    D.log(TAG, "Secure password storing failed", e);
                }
                if (!mActivity.accountExists()) {
                    mActivity.addAccount(mUsername, new String(mPassword));
                } else {
                    mActivity.changeAccount(mUsername, new String(mPassword));
                }
                mUsername = null;
                Arrays.fill(mPassword, '0');
                mPassword = null;
                mActivity.mPasswordEditText.setText(null);
                hideProgress();
                mActivity.exit();
            } else {
                hideProgress();
            }
        }

        private void hideProgress() {
            //TODO: call in onDestroy also
            if (mProgressDialog != null && !mActivity.isFinishing()) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }

    }

    private boolean accountExists() {
        return AuthenticatorService.accountExists(getApplicationContext());
    }

    private void login() {
        new LoginTask(this, mLoginEditText.getText().toString(), mPasswordEditText.getText().toString(), mServerArea).execute();
    }

    private boolean afterLogin(BL.LoginResult result) {
        D.log(TAG, "[afterLogin] result.success: %s", result.isSuccess());
        if (!result.isSuccess()) {
            D.log(TAG, "[afterLogin] result.error_code: %s", result.getResultCode());
            if ((MainReceiver.getNetworkClass(this) == MainReceiver.NETWORK_TYPE_NOT_CONNECTED)) {
                mLoginFailure.setText(R.string.login_no_network);
            } else {
                if (result.getResultCode() == BL.ResultCode.ERROR_AUTH_FAILED) {
                    mLoginFailure.setText(R.string.login_failure);
                } else {
                    mLoginFailure.setText(getString(R.string.login_error));
                }
            }
            mLoginFailure.setVisibility(View.VISIBLE);
            return false;
        }
        return true;
    }

    protected void addAccount(String username, String password) {
        D.log(TAG, "[addAccount]");
        String AUTHORITY = getString(R.string.CONTENT_AUTHORITY);
        Account account = new Account(username, DodicallAccountAuthenticator.ACCOUNT_TYPE);
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        accountManager.addAccountExplicitly(account, password, null);
        ContentResolver.setIsSyncable(account, AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, AUTHORITY, false);
        Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, DodicallAccountAuthenticator.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK);
    }

    protected void changeAccount(String username, String password) {
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account account = accountManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE))[0];
        if (TextUtils.equals(account.name, username) && TextUtils.equals(accountManager.getPassword(account), password)) {
            return;
        }
        accountManager.removeAccount(account, null, null);
        addAccount(username, password);
//        accountManager.setPassword(account, password);
//        accountManager.renameAccount(account, username, null, null);
    }

    private void exit() {
        D.log(TAG, "[exit]");
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (upIntent != null && NavUtils.shouldUpRecreateTask(this, upIntent) ||
                TextUtils.equals(getIntent().getAction(), ACTION_RELOGIN)) {
            //probably, this activity started from settings app
            D.log(TAG, "[exit] started from settings app");
            finish();
        } else {
            /*TaskStackBuilder.create(this).addNextIntentWithParentStack(
                    new Intent(getApplicationContext(), MainActivity.class)
            ).startActivities();*/

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            D.log(TAG, "[exit] start MainActivity");
        }
    }

    private static String appendCountryAndLanguageToUrl(String url, String locale) {
        return (
                url.replaceAll(
                        "\\$\\{COUNTRY\\}", locale.startsWith("ru") ? "ru" : "gb"
                ).replaceAll(
                        "\\$\\{LANG\\}", locale.startsWith("ru") ? "ru" : "en"
                )
        );
    }

    private String getRegistrationUrl() {
        return String.format(
                "%s%s",
                mServerAreas.get(mServerArea).getLcUrl(),
                appendCountryAndLanguageToUrl(
                        mServerAreas.get(mServerArea).getReg(),
                        Utils.getLocale(getApplicationContext())
                )
        );
    }

    private String getRestorePasswordUrl() {
        return String.format(
                "%s%s",
                mServerAreas.get(mServerArea).getLcUrl(),
                appendCountryAndLanguageToUrl(
                        mServerAreas.get(mServerArea).getForgotPwd(),
                        Utils.getLocale(getApplicationContext())
                )
        );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.registration:
                Utils.launchUrl(this, getRegistrationUrl());
                break;
            case R.id.password_restore:
                Utils.launchUrl(this, getRestorePasswordUrl());
                break;
            case R.id.login_button:
                login();
                break;
            case R.id.big_logo:
                switchServerType();
                break;
        }
    }

    private static String getAreaName(ServerAreaModelWrapper serverAreaModel, String locale) {
        if (TextUtils.isEmpty(locale)) {
            return serverAreaModel.getNameEn();
        }
        if (locale.startsWith("ru")) {
            return serverAreaModel.getNameRu();
        } else if (locale.startsWith("en")) {
            return serverAreaModel.getNameEn();
        } else {
            return serverAreaModel.getNameEn();
        }

    }

    private void setServerArea(ServerAreaModelWrapper serverAreaModel) {
        D.log(TAG, "[setServerArea] area: %d", serverAreaModel.getKey());
        mServerArea = serverAreaModel.getKey();
        setVersion(serverAreaModel);
        Preferences.get(getApplicationContext()).
                edit().
                putInt(Preferences.Fields.PREF_LAST_SERVER_AREA, mServerArea).
                putString(Preferences.Fields.PREF_LAST_SERVER_AREA_URL, serverAreaModel.getLcUrl()).
                apply();
        mGlobalSettings.setArea(mServerArea);
    }

    private void setVersion(ServerAreaModelWrapper serverAreaModel) {
        String versionName = Utils.formatVersion(this, mServerArea, serverAreaModel);
        mVersion.setText(versionName);
    }

    private ServerAreaModelWrapper getCurrentServerArea() {
        for (int i = 0; i < mServerAreas.size(); i++) {
            ServerAreaModelWrapper areaModelWrapper = mServerAreas.get(i);
            if (areaModelWrapper.getKey() == mServerArea) {
                return areaModelWrapper;
            }
        }

        if (!mServerAreas.isEmpty()) {
            return mServerAreas.get(0);
        }

        throw new IllegalStateException(String.format("Server area with key %s doesn\'t exist", mServerArea));
    }

    private static class ServerAreaWrapper {

        private ServerAreaModelWrapper mServerAreaModel;
        private String mLocale;

        private ServerAreaWrapper(ServerAreaModelWrapper serverAreaModel, String locale) {
            mServerAreaModel = serverAreaModel;
            mLocale = locale;
        }

        private ServerAreaModelWrapper getArea() {
            return mServerAreaModel;
        }

        @Override
        public String toString() {
            return getAreaName(mServerAreaModel, mLocale);
        }

    }

    private void showAreaSelectionDialog() {
        final ArrayAdapter<ServerAreaWrapper> serverAreaAdapter = new ArrayAdapter<>(
                this, android.R.layout.select_dialog_singlechoice
        );
        final String locale = Utils.getLocale(this);
        int position = 0;
        for (int i = 0; i < mServerAreas.size(); i++) {
            ServerAreaModelWrapper areaModelWrapper = mServerAreas.get(i);
            if (areaModelWrapper.getKey() == mServerArea) {
                position = i;
            }
            serverAreaAdapter.add(new ServerAreaWrapper(areaModelWrapper, locale));
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.available_areas_dialog_title)
                .setSingleChoiceItems(serverAreaAdapter, position, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final ListView listView = ((AlertDialog) dialog).getListView();
                    final int pos = listView.getCheckedItemPosition();
                    final ServerAreaWrapper serverAreaWrapper =
                            (ServerAreaWrapper) listView.getAdapter().getItem(pos);
                    setServerArea(serverAreaWrapper.getArea());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void switchServerType() {
        D.log(TAG, "[switchServerType] counter: %d;", switchServerCounter);
        if (++switchServerCounter == 5) {
            switchServerCounter = 0;
            showAreaSelectionDialog();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switchLanguage(((LangItem) mLangAdapter.getItem(position)).locale);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        D.log(TAG, "[onEditorAction] id: %d; action: %d;", v.getId(), actionId);
        if (v.getId() == R.id.password && EditorInfo.IME_ACTION_DONE == actionId) {
            hideKeyboard(v);
            login();
            return true;
        }
        return false;
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusView = getCurrentFocus();
        if (focusView != null) {
            inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
        }
    }

    //TODO: make static, user Resources.getSystem()?
    private int getNavigationBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private int getStatusBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    //holy shit
    @Override
    public void onGlobalLayout() {
        if (mContent != null) {
            D.log(
                    TAG, "[onGlobalLayout] diff_height: %d; navigationBarHeight: %d; statusBarHeight: %d;",
                    (mContent.getRootView().getHeight() - mContent.getHeight()),
                    getNavigationBarHeight(), getStatusBarHeight()
            );
            boolean currentKeyboardHiddenState =
                    (mContent.getRootView().getHeight() - mContent.getHeight()) > getNavigationBarHeight() + getStatusBarHeight();
            if (previousKeyboardHiddenState == currentKeyboardHiddenState) {
                return;
            }
            previousKeyboardHiddenState = currentKeyboardHiddenState;
            if (currentKeyboardHiddenState) {
                D.log(TAG, "[onGlobalLayout] keyboard_debug open");
                mLangSpinner.setVisibility(View.GONE);
                if (mLogo != null) {
                    mLogo.setVisibility(View.GONE);
                }
                if (mKeyboardHackWrapper != null) {
                    mKeyboardHackWrapper.setGravity(Gravity.CENTER_VERTICAL);
                }
            } else {
                D.log(TAG, "[onGlobalLayout] keyboard_debug hidden");
                mLangSpinner.setVisibility(View.VISIBLE);
                if (mLogo != null) {
                    mLogo.setVisibility(View.VISIBLE);
                }
                if (mKeyboardHackWrapper != null) {
                    mKeyboardHackWrapper.setGravity(Gravity.NO_GRAVITY);
                }
            }
        }
    }

}
