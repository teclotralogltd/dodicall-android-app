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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import ru.swisstok.dodicall.DodicallApplication;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.adapter.TabAdapter;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.fragment.BaseFragment;
import ru.swisstok.dodicall.fragment.ChatsFragment;
import ru.swisstok.dodicall.fragment.ContactsFragment;
import ru.swisstok.dodicall.fragment.DialpadFragment;
import ru.swisstok.dodicall.fragment.HistoryFragment;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.receiver.MainReceiver;
import ru.swisstok.dodicall.service.RegistrationGcmService;
import ru.swisstok.dodicall.task.GetMeAsyncTaskLoader;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.NotificationsUtils;
import ru.swisstok.dodicall.util.OnBackPressedListener;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.BadgeView;
import ru.swisstok.dodicall.view.RoundedImageView;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.GlobalApplicationSettingsModel;
import ru.uls_global.dodicall.NetworkStateModel;
import ru.uls_global.dodicall.ServerAreasList;

@RuntimePermissions
public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        BaseFragment.FragmentActionListener {

    private static final String TAG = "MainActivity";
    //    public static final String INIT_SPINNER_POSITION = "init_spinner_position";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 900;
    private static final int LOADER_ID_ME = 902;

    public static final String EXTRA_OPEN_CHATS = "openChats";
    public static final String EXTRA_OPEN_HISTORY = "openHistory";
    private static final String PREF_NAME_SPINNER = "Spinner";
    private static final String PREF_KEY_POS = "Position";

    public interface ContactsCallback {
        void filterChanged(ToolBarSpinnerAdapter.BaseItem filter);

        void invitesCounterChanged(int newCount);
    }

    private static List<TabAdapter.TabSpec> sTabs = new ArrayList<>();

    static {
        sTabs.add(new TabAdapter.TabSpec(
                ContactsFragment.class, R.string.tab_title_contacts,
                R.drawable.contacts_tab_ic, null,
                ContactsFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.WithoutNotification
        ));
        sTabs.add(new TabAdapter.TabSpec(
                HistoryFragment.class, R.string.tab_title_history,
                R.drawable.history_tab_ic, null,
                HistoryFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.MissedCall
        ));
        sTabs.add(new TabAdapter.TabSpec(
                ChatsFragment.class, R.string.tab_title_chats,
                R.drawable.chats_tab_ic, null,
                ChatsFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.Chat
        ));
        sTabs.add(new TabAdapter.TabSpec(
                DialpadFragment.class, R.string.tab_title_dialpad,
                R.drawable.dialpad_tab_ic, null,
                DialpadFragment.TAB_POSITION,
                NotificationsUtils.NotificationType.WithoutNotification
        ));
    }

    private static List<ToolBarSpinnerAdapter.BaseItem> sContactFilters = new ArrayList<>();

    static {
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_all, -1, ToolBarSpinnerAdapter.FILTER_ALL
        ));
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_dodicall, -1, ToolBarSpinnerAdapter.FILTER_DDC
        ));
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_phone, -1, ToolBarSpinnerAdapter.FILTER_PHONE
        ));
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_saved, -1, ToolBarSpinnerAdapter.FILTER_SAVED
        ));
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_locked, -1, ToolBarSpinnerAdapter.FILTER_BLOCKED
        ));
        sContactFilters.add(new ToolBarSpinnerAdapter.BaseItem(
                R.string.contacts_filter_white, -1, ToolBarSpinnerAdapter.FILTER_WHITE
        ));
    }

    private MainReceiver mMainReceiver;
    private List<BadgeView> mCounters = new ArrayList<>();
    private ContactsCallback mContactsCallback;
    private int mSpinnerPosition;

    private TabLayout mTabLayout;
    private DrawerLayout mDrawer;
    private LinearLayout mBalanceWrapper;
    private TextView mNavStatus;
    private TextView mNavBalanceText;
    private ViewPager mViewPager;
    private RoundedImageView mAvatarView;
    private TextView mProfileNameText;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(action, DataProvider.ACTION_CHATS_UPDATED) ||
                    TextUtils.equals(action, DataProvider.ACTION_CHATS_DELETED)) {
                updateUnreadMessagesCount();
            } else if (TextUtils.equals(MainReceiver.ACTION_MAIN_STATUS_UPDATE, action)) {
                ContactStatus status = (ContactStatus) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                D.log(TAG, "[onReceive] main_status_update; base: %d; ext: %s", status.getStatusId(), status.getExtraStatus());
                StatusesAdapter.setupStatusView(mNavStatus, status.getStatusId(), status.getExtraStatus());
            } else if (TextUtils.equals(DataProvider.ACTION_HISTORY_UPDATED, action)) {
                updateMissedCallsCount();
                DataProvider.updateBadge(MainActivity.this);
            } else if (TextUtils.equals(BaseManager.AVATAR_LOADED, action)) {
                Contact contact = (Contact) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                if (contact.iAm && mAvatarView != null) {
                    getSupportLoaderManager().destroyLoader(LOADER_ID_ME);
                    getSupportLoaderManager().initLoader(LOADER_ID_ME, null, mMeLoaderCallback);
                }
            }
        }
    };

    private LoaderManager.LoaderCallbacks<Contact> mMeLoaderCallback = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            return new GetMeAsyncTaskLoader(MainActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            mProfileNameText.setText(Utils.formatAccountFullName(data));
            mAvatarView.setUrl(data.avatarPath);
            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D.log(TAG, "[onCreate]");
        DodicallApplication.setRun();

        SharedPreferences spinnerPref = getSharedPreferences(PREF_NAME_SPINNER, MODE_PRIVATE);
        mSpinnerPosition = spinnerPref.getInt(PREF_KEY_POS, ToolBarSpinnerAdapter.FILTER_DDC);

        setContentView(R.layout.activity_main);
        setupNavigation();
        setupReceiver();
        MainActivityPermissionsDispatcher.startDangerousFunctionsWithCheck(this);
        if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationGcmService.class);
            startService(intent);
        }

        LocalBroadcast.registerReceiver(this, mBroadcastReceiver,
                BaseManager.AVATAR_LOADED,
                DataProvider.ACTION_CHATS_UPDATED,
                DataProvider.ACTION_CHATS_DELETED,
                MainReceiver.ACTION_MAIN_STATUS_UPDATE,
                DataProvider.ACTION_HISTORY_UPDATED);

        getSupportLoaderManager().initLoader(0, null, this);

        if (getIntent().hasExtra(EXTRA_OPEN_CHATS)) {
            NotificationsUtils.cancelNotification(this, NotificationsUtils.NotificationType.Chat);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setCurrentItem(getChatTabIndex());
        } else if (getIntent().hasExtra(EXTRA_OPEN_HISTORY)) {
            NotificationsUtils.cancelNotification(this, NotificationsUtils.NotificationType.MissedCall);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setCurrentItem(getHistoryTabIndex());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_OPEN_CHATS)) {
            NotificationsUtils.cancelNotification(this, NotificationsUtils.NotificationType.Chat);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setCurrentItem(getChatTabIndex());
        } else if (intent.hasExtra(EXTRA_OPEN_HISTORY)) {
            NotificationsUtils.cancelNotification(this, NotificationsUtils.NotificationType.MissedCall);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setCurrentItem(getHistoryTabIndex());
        }
    }

    @NeedsPermission({Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO})
    void startDangerousFunctions() {
        DataProvider.startPhonebookSync(getContentResolver());
    }

    @OnShowRationale({Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO})
    void showPermissionsExplanation(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permissions_explanation)
                .setPositiveButton(android.R.string.yes, ((dialog, which) -> request.proceed()))
                .setNegativeButton(android.R.string.no, ((dialog, which) -> request.cancel()))
                .show();
    }

    @OnPermissionDenied({Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO})
    void permissionsDenied() {
        Toast.makeText(
                getApplicationContext(), R.string.close_explanation, Toast.LENGTH_SHORT
        ).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(
                this, requestCode, grantResults
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcast.unregisterReceiver(this, mBroadcastReceiver);
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                D.log(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        D.log(TAG, "[onResume]");
        registerReceiver(mMainReceiver, MainReceiver.ACTION_FILTER);
        updateStatus();
        updateBalance();

        mTabLayout.postDelayed(this::updateUnreadMessagesCount, 200);
        mTabLayout.postDelayed(this::updateMissedCallsCount, 200);
    }

    private void updateUnreadMessagesCount() {
        int newMessagesCount = BusinessLogic.GetInstance().GetNewMessagesCount();
        setTabCounter(ChatsFragment.TAB_POSITION, newMessagesCount);
    }

    private void updateMissedCallsCount() {
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                return BL.getAllMissedCalls();
            }

            @Override
            protected void onPostExecute(Integer missedCall) {
                setTabCounter(HistoryFragment.TAB_POSITION, missedCall);
            }
        }.execute();
    }

    @Override
    public void onPause() {
        super.onPause();
        D.log(TAG, "[onPause]");
        if (mMainReceiver != null) {
            unregisterReceiver(mMainReceiver);
        }
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        outState.putInt(INIT_SPINNER_POSITION, mSpinnerPosition);
//        super.onSaveInstanceState(outState);
//    }

    private void setupNavigation() {
        setupToolbar();
        setupTabs();
        setupTabCounters();
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowTitleEnabled(false);
        }
        setupContactsSpinner(toolbar);
        setupDrawer(toolbar);
    }

    private void setupContactsSpinner(Toolbar toolbar) {
        View spinnerContainer = LayoutInflater.from(this).inflate(
                R.layout.toolbar_spinner, toolbar, false
        );
        toolbar.addView(spinnerContainer);
        ToolBarSpinnerAdapter spinnerAdapter = new ToolBarSpinnerAdapter(this, sContactFilters);
        Spinner contactSpinner = (Spinner) spinnerContainer.findViewById(R.id.toolbar_spinner);
        contactSpinner.setAdapter(spinnerAdapter);

        if (mSpinnerPosition > 0) {
            contactSpinner.setSelection(mSpinnerPosition);
        }

        contactSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                D.log(TAG, "[spinner.onItemSelected] position: %d", position);
                mSpinnerPosition = position;
                setFilter((ToolBarSpinnerAdapter.BaseItem) parent.getItemAtPosition(position));

                getSharedPreferences(PREF_NAME_SPINNER, MODE_PRIVATE)
                        .edit()
                        .putInt(PREF_KEY_POS, mSpinnerPosition)
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public boolean canCreateContacts() {
        return true;
    }

    public boolean withContactsSpinner() {
        return true;
    }

    public int getCurrentFilter() {
        return mSpinnerPosition;
    }

    private void setFilter(ToolBarSpinnerAdapter.BaseItem filterItem) {
        if (mContactsCallback != null) {
            mContactsCallback.filterChanged(filterItem);
        }
    }

    public void setFilterCallback(ContactsCallback contactsCallback) {
        mContactsCallback = contactsCallback;
    }

    protected void setupDrawer(Toolbar toolbar) {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerLayout = navigationView.getHeaderView(0);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        mDrawer.addDrawerListener(toggle);
        final CountDownTimer timer = new CountDownTimer(30000, 30000) {
            @Override
            public void onTick(long millisUntilFinished) {
                D.log(TAG, "[setupDrawer][onTick]");
                updateBalance();
            }

            @Override
            public void onFinish() {
                start();
            }
        };
        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                updateBalance();
                timer.start();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                timer.cancel();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
        toggle.syncState();
        TextView versionTextView = ((TextView) findViewById(R.id.app_version_number));
        if (versionTextView != null) {
            new AsyncTask<Void, Void, Void>() {
                GlobalApplicationSettingsModel globalSettings;
                ServerAreasList serverAreas;
                int serverArea;

                @Override
                protected Void doInBackground(Void... params) {
                    globalSettings = BusinessLogic.GetInstance().GetGlobalApplicationSettings();
                    serverAreas = BusinessLogic.GetInstance().RetrieveAreas();
                    serverArea = globalSettings.getArea();
                    return null;
                }

                @Override
                public void onPostExecute(Void result) {
                    versionTextView.setText(Utils.formatVersion(getApplicationContext(), serverArea, Utils.getAreaByKey(serverAreas, serverArea)));
                }
            }.execute();
        }

        headerLayout.findViewById(R.id.profile_info_section).setOnClickListener(this);
        headerLayout.findViewById(R.id.nav_drawer_preferences).setOnClickListener(this);
        headerLayout.findViewById(R.id.nav_drawer_logout).setOnClickListener(this);
        mNavStatus = (TextView) headerLayout.findViewById(R.id.profile_status);
        mAvatarView = (RoundedImageView) headerLayout.findViewById(R.id.nav_profile_avatar);
        mProfileNameText = (TextView) headerLayout.findViewById(R.id.profile_name);
        mNavBalanceText = (TextView) headerLayout.findViewById(R.id.balance);
        mBalanceWrapper = (LinearLayout) headerLayout.findViewById(R.id.drawer_balance_wrapper);

        getSupportLoaderManager().initLoader(LOADER_ID_ME, null, mMeLoaderCallback);
    }

    protected List<TabAdapter.TabSpec> getTabs() {
        return sTabs;
    }

    private void setupTabs() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        TabAdapter adapter = new TabAdapter(getSupportFragmentManager(), getApplicationContext(), getTabs(), this);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mViewPager.setAdapter(adapter);
        mTabLayout = (TabLayout) findViewById(R.id.tablayout);
        setupTabs(mTabLayout);
    }

    protected void setupTabs(TabLayout tabLayout) {
        tabLayout.setupWithViewPager(mViewPager);
        TabLayout.Tab currentTab;
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            currentTab = tabLayout.getTabAt(i);
            if (currentTab != null) {
                currentTab.setCustomView(R.layout.tab);
                if (i == 0) {
                    currentTab.getCustomView().setSelected(true);
                }
                currentTab.setIcon(getTabs().get(i).icon);
            }
        }
        tabLayout.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        setupActivityNotifications();
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        removeActivityNotifications();
                        if (tab.getPosition() == HistoryFragment.TAB_POSITION) {
                            new Thread(BL::setAllCallHistoryRead).start();
                        }
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                    }
                }
        );
    }

    @Nullable
    @Override
    protected NotificationsUtils.NotificationType getNotificationType() {
        return getTabs().get(mTabLayout.getSelectedTabPosition()).notificationType;
    }

    @Override
    protected boolean isWithNotificationUpdate() {
        return mTabLayout.getSelectedTabPosition() == HistoryFragment.TAB_POSITION;
    }

    private void setupReceiver() {
        mMainReceiver = new MainReceiver() {
            @Override
            public void onNetworkChanged(int networkType) {
                D.log(TAG, "[onReceive] networkType: %s", networkType);
                TabLayout.Tab chatTab = getChatTab();
                TabLayout.Tab dialpadTab = getDialpadTab();
                final NetworkStateModel networkState =
                        BusinessLogic.GetInstance().GetNetworkState();
                D.log(TAG, "[onReceive] chatStatus: %s; voipStatus: %s;", networkState.getChatStatus(), networkState.getVoipStatus());
                if (chatTab != null && chatTab.getIcon() != null) {
                    chatTab.getIcon().setLevel(
                            !networkState.getChatStatus() ?
                                    MainReceiver.NETWORK_TYPE_NOT_CONNECTED : networkType
                    );
                }
                if (dialpadTab != null && dialpadTab.getIcon() != null) {
                    dialpadTab.getIcon().setLevel(
                            !networkState.getVoipStatus() ?
                                    MainReceiver.NETWORK_TYPE_NOT_CONNECTED : networkType
                    );
                }
            }
        };
    }

    protected int getHistoryTabIndex() {
        return HistoryFragment.TAB_POSITION;
    }

    protected int getChatTabIndex() {
        return ChatsFragment.TAB_POSITION;
    }

    protected TabLayout.Tab getChatTab() {
        return getTabAt(ChatsFragment.TAB_POSITION);
    }

    protected TabLayout.Tab getDialpadTab() {
        return getTabAt(DialpadFragment.TAB_POSITION);
    }

    protected TabLayout.Tab getTabAt(int position) {
        return mTabLayout.getTabAt(position);
    }

    protected boolean isShowCounters() {
        return true;
    }

    private void setupTabCounters() {
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            final int badgePos = i;
            BadgeView badge = new BadgeView(getApplicationContext(), mTabLayout, i);
            badge.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
            mCounters.add(badge);
            mTabLayout.getTabAt(i).getCustomView().getViewTreeObserver()
                    .addOnGlobalLayoutListener(() -> {
                        TabLayout.Tab tab = mTabLayout.getTabAt(badgePos);
                        int iconWidth = tab.getIcon().getIntrinsicWidth();
                        int iconHeight = tab.getIcon().getIntrinsicHeight();
                        int tabWidth = tab.getCustomView().getMeasuredWidth();
                        int tabHeight = tab.getCustomView().getMeasuredHeight();
                        int horizontalMargin = tabWidth / 2 + iconWidth / 5;
                        int verticalMargin = tabHeight / 2 - 4 * iconHeight / 5;
                        if (mCounters.get(badgePos).getHorizontalBadgeMargin() == horizontalMargin) {
                            return;
                        }
                        mCounters.get(badgePos).setBadgeMargin(horizontalMargin, verticalMargin);
                    });
        }
    }

    private void setTabCounter(int tabPosition, int value) {
        if (isShowCounters()) {
            BadgeView counter = mCounters.get(tabPosition);
            if (counter != null) {
                if (value > 0) {
                    String s = value > 99 ? "99+" : String.valueOf(value);
                    counter.setText(s);
                    counter.show();
//                mTabLayout.getTabAt(tabPosition).getIcon()
                } else {
                    counter.hide();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            boolean interruptBack = false;

            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            if (CollectionUtils.isNotEmpty(fragments)) {
                int page = mViewPager.getCurrentItem();
                Class<? extends Fragment> clazz = getTabs().get(page).tClass;

                for (Fragment fragment : fragments) {
                    if (fragment != null && fragment.getClass() == clazz && fragment instanceof OnBackPressedListener) {
                        interruptBack = ((OnBackPressedListener) fragment).onBackPressed();
                    }
                }
            }

            if (!interruptBack) {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void startActivityFromDrawer(Class activity) {
        startActivityFromDrawer(activity, null);
    }

    private void startActivityFromDrawer(Class activity, @Nullable String action) {
        final Intent intent = new Intent(getApplicationContext(), activity);
        if (!TextUtils.isEmpty(action)) {
            intent.setAction(action);
        }

        startActivityFromDrawer(intent);
    }

    private void startActivityFromDrawer(@NonNull Intent intent) {
        mDrawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                startActivity(intent);
                mDrawer.removeDrawerListener(this);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        mDrawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_drawer_preferences:
                startActivityFromDrawer(SettingsActivity.class);
                break;
            case R.id.nav_drawer_logout:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.alert_dialog_message_logout)
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
                            try {
                                BL.logout(MainActivity.this, getContentResolver());
                            } catch (Exception e) {
                                D.log(TAG, "Logout is failed", e);
                            }
                            DodicallApplication.clearRun();

                            Intent intent = new Intent(this, SplashActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        }).show();
                break;
            case R.id.profile_info_section: {
                startActivityFromDrawer(
                        ProfileActivity.class, ProfileActivity.ACTION_OPEN_PERSONAL_PROFILE
                );
                break;
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this, DataProvider.INVITES_COUNT_URI, null, null, null, null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            int invitesCount = data.getInt(
                    data.getColumnIndex(DataProvider.COLUMN_INVITES_COUNT)
            );
            D.log(TAG, "[onLoadFinished] invitesCount: %d", invitesCount);
            updateInvitesCount(invitesCount);
        } else {
            updateInvitesCount(0);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onContactCall(Contact contact) {
        OutgoingCallActivity.start(this, contact);
    }

    @Override
    public void onOpenContact(Contact contact) {
        ProfileActivity.openProfile(this, contact);
    }

    @Override
    public void onCallToNumber(String number) {
        OutgoingCallActivity.start(this, number);
    }

    @Override
    public void onOpenHistory(CallHistory callHistory) {
        CallHistoryDetailActivity.start(this, callHistory);
    }

    @Override
    public void onOpenChat(Chat chat) {
        startActivity(ChatActivity.newIntent(this, chat));
    }

    private void updateInvitesCount(int invitesCount) {
        setTabCounter(ContactsFragment.TAB_POSITION, invitesCount);
        if (mContactsCallback != null) {
            mContactsCallback.invitesCounterChanged(invitesCount);
        }
        invalidateOptionsMenu();
    }

    private void updateBalance() {
        D.log(TAG, "[updateBalance]");
        if (mNavBalanceText != null) {
            new AsyncTask<Void, Void, Balance>() {
                @Override
                protected Balance doInBackground(Void... params) {
                    return BL.getBalance();
                }

                @Override
                public void onPostExecute(Balance balance) {
                    ContactsManagerImpl.getInstance().updateBalance(balance);
                    D.log(TAG, "[updateBalance] complete");
                    if (balance != null) {
                        mBalanceWrapper.setVisibility(View.VISIBLE);
                        if (balance.isSuccessful()) {
                            mNavBalanceText.setText(Utils.getBalanceText(balance));
                        }
                    } else {
                        mBalanceWrapper.setVisibility(View.GONE);
                    }
                }
            }.execute();
        }
    }

    private void updateStatus() {
        D.log(TAG, "[updateStatus]");
        if (mNavStatus != null) {
            new AsyncTask<Void, Void, Contact>() {
                @Override
                protected Contact doInBackground(Void... params) {
                    D.log(TAG, "[updateStatus] start");
                    return ContactsManagerImpl.getInstance().getMyContact();
                }

                @Override
                public void onPostExecute(Contact contact) {
                    ContactStatus contactStatus = contact.contactStatus;
                    D.log(TAG, "[updateStatus] done; status: %s; ext_status: %s", contactStatus.getStatusId(), contactStatus.getExtraStatus());
                    StatusesAdapter.setupStatusView(mNavStatus, contactStatus.getStatusId(), contactStatus.getExtraStatus());
                }
            }.execute();
        }
    }
}
