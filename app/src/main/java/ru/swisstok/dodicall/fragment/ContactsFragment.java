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

package ru.swisstok.dodicall.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ChatActivity;
import ru.swisstok.dodicall.activity.CreateProfileActivity;
import ru.swisstok.dodicall.activity.DodicallSearchActivity;
import ru.swisstok.dodicall.activity.MainActivity;
import ru.swisstok.dodicall.activity.SelectContactToTransferActivity;
import ru.swisstok.dodicall.adapter.BaseAdapter;
import ru.swisstok.dodicall.adapter.ContactAdapter;
import ru.swisstok.dodicall.adapter.ToolBarSpinnerAdapter;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.BaseManager;
import ru.swisstok.dodicall.manager.ContactsManager;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.task.ContactActionAsyncTask;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.D;
import ru.swisstok.dodicall.util.LocalBroadcast;
import ru.swisstok.dodicall.util.Logger;
import ru.swisstok.dodicall.view.AlphabetContactsView;
import ru.uls_global.dodicall.BusinessLogic;

public class ContactsFragment extends BaseFragment implements
        LoaderManager.LoaderCallbacks<List<Object>>,
        ContactAdapter.ContactActionListener, MainActivity.ContactsCallback, SearchView.OnQueryTextListener {

    public static final int TAB_POSITION = 0;

    private static final String TAG = "ContactsFragment";
    private static final int LOADER_ID = 0;

    public static final String FILTER_TYPE = "filter_type";
    protected static final String FILTER_QUERY_TEXT = "filter_query_text";
    private static final String FORCE_UPDATE = "force_update";

    public static final String ARG_DISABLED_CONTACTS_LIST = "arg.DisableContactsList";
    public static final String BUTTONS_CONTACTS_LIST = "arg.ButtonsContactsList";
    public static final String REQUESTS_CONTACTS_LIST = "arg.RequestsContactsList";
    public static final String INVITES_CONTACTS_LIST = "arg.InvitesContactsList";

    private ContactsManager mContactsManager;
    private ContactAdapter mAdapter;
    private TextView mEmpty;
    private LinearLayout mContent;
    private ProgressBar mProgress;
    private RecyclerView mListView;
    private ViewGroup mFabMenu;
    private FloatingActionButton mFab;

    private int mCurrentFilter = ToolBarSpinnerAdapter.FILTER_ALL;
    private boolean mWithRequests;
    private boolean mWithInvites;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ContactsManager.CONTACTS_UPDATED) ||
                    action.equals(BaseManager.AVATAR_LOADED) ||
                    action.equals(ContactsManager.ACTION_USERS_STATUSES_UPDATED) ||
                    action.equals(ContactsManager.CONTACT_REMOVED) ||
                    action.equals(ContactsManager.INVITE_READ)) {
                if (isAdded()) {
                    Log.d("Con", "Restart with action:" + action);
                    restartWithNewFilter(mCurrentFilter, null, false);
                }
            } else if (action.equals(ContactsManager.CONTACTS_ACCEPTED)) {
                Contact contact = (Contact) intent.getSerializableExtra(BaseManager.EXTRA_DATA);
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    Object item = mAdapter.getItem(i);
                    if (item instanceof Contact) {
                        if (contact.dodicallId.equals(((Contact) item).dodicallId)) {
                            mAdapter.setItem(i, contact);
                            mAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                }
            }
        }
    };

    public ContactsFragment() {
        mContactsManager = ContactsManagerImpl.getInstance();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) context).setFilterCallback(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

        mWithRequests = getArguments().getBoolean(REQUESTS_CONTACTS_LIST, true);
        mWithInvites = getArguments().getBoolean(INVITES_CONTACTS_LIST, true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        D.log(TAG, "[onCreateView]");
        View view = inflater.inflate(R.layout.contacts_tab_fragment, container, false);
        mListView = (RecyclerView) view.findViewById(R.id.list);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setAdapter(setupAdapter());
        mEmpty = (TextView) view.findViewById(android.R.id.empty);
        mContent = (LinearLayout) view.findViewById(R.id.content);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);

        AlphabetContactsView alphabetContactsView = (AlphabetContactsView) view.findViewById(R.id.contacts_alphabet_indexer);
        String language = BusinessLogic.GetInstance().GetUserSettings().getGuiLanguage();

        alphabetContactsView.setupLanguage(language);
        alphabetContactsView.setAlphabetListener(letter -> {
            int position = mAdapter.getSectionPositionForLetter(letter);
            if (position != BaseAdapter.INVALID_POSITION) {
                mListView.getLayoutManager().scrollToPosition(position);
            }
        });

        mFabMenu = (ViewGroup) view.findViewById(R.id.fab_menu);

        FloatingActionButton newContactFab = (FloatingActionButton) view.findViewById(R.id.new_contact_fab);
        newContactFab.setOnClickListener(v -> {
            CreateProfileActivity.start(getContext());
            mFab.performClick();
        });

        FloatingActionButton searchContactFab = (FloatingActionButton) view.findViewById(R.id.search_contact_fab);
        searchContactFab.setOnClickListener(v -> {
            DodicallSearchActivity.start(getContext());
            mFab.performClick();
        });

        mFabMenu.setOnClickListener(v -> mFab.performClick());
        mFab = (FloatingActionButton) view.findViewById(R.id.fab);
        mFab.setOnClickListener(v -> {
            boolean isMenuVisible = mFabMenu.getVisibility() == View.VISIBLE;
            mFab.animate().
                    rotation(isMenuVisible ? 0 : 45).
                    setDuration(100).
                    setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFabMenu.setVisibility(isMenuVisible ? View.INVISIBLE : View.VISIBLE);
                            mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getActivity(), isMenuVisible ? R.color.fab_normal : R.color.fab_pressed)));
                        }
                    }).
                    start();
        });

        boolean canCreateContacts = ((MainActivity) getActivity()).canCreateContacts();
        if (canCreateContacts) {
            mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0)
                        mFab.hide();
                    else if (dy < 0)
                        mFab.show();
                }
            });
        } else {
            mFab.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        D.log(TAG, "[onActivityCreated]");
        super.onActivityCreated(savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            mCurrentFilter = ((MainActivity) getActivity()).getCurrentFilter();
        }

        restartWithNewFilter(mCurrentFilter, null, true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        D.log(TAG, "[onCreateOptionsMenu]");
        boolean withSpinner = ((MainActivity) getActivity()).withContactsSpinner();
        View toolbar = getActivity().findViewById(R.id.toolbar_spinner);
        toolbar.setVisibility(withSpinner ? View.VISIBLE : View.GONE);

        if (getActivity() instanceof SelectContactToTransferActivity) {
            menuInflater.inflate(R.menu.select_contacts_tab, menu);
            final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setOnQueryTextListener(this);
        } else if (getActivity() instanceof MainActivity) {
            menuInflater.inflate(R.menu.contacts_tab, menu);

            final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(this);

                searchView.setOnSearchClickListener(v -> toolbar.setVisibility(View.INVISIBLE));
                searchView.setOnCloseListener(() -> {
                    toolbar.setVisibility(withSpinner ? View.VISIBLE : View.GONE);
                    return false;
                });
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcast.registerReceiver(getActivity(), mReceiver,
                ContactsManager.CONTACTS_UPDATED, BaseManager.AVATAR_LOADED, ContactsManager.ACTION_USERS_STATUSES_UPDATED, ContactsManager.CONTACT_REMOVED, ContactsManager.INVITE_READ);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcast.unregisterReceiver(getActivity(), mReceiver);
        getLoaderManager().destroyLoader(LOADER_ID);
    }

    @Override
    public Loader<List<Object>> onCreateLoader(int id, Bundle args) {
        int filter = args.getInt(FILTER_TYPE);
        String query = args.getString(FILTER_QUERY_TEXT);
        boolean forceUpdate = args.getBoolean(FORCE_UPDATE);
        Log.d("Con", "Create");
        return new AsyncTaskLoader<List<Object>>(getActivity()) {
            @Override
            public List<Object> loadInBackground() {
                Log.d("Con", "in back");
                if (forceUpdate) {
                    mContactsManager.cleanNewAccepts();
                }
                Logger.onOperationStart("GetContactsProvider");
                List<Contact> contacts = mContactsManager.getContacts(filter, query, mWithRequests, mWithInvites);
                Logger.onOperationEnd("GetContactsProvider", contacts.size());
                Map<String, Integer> mLetter = new HashMap<>();
                List<Object> result = new ArrayList<>();
                List<Contact> requests = new ArrayList<>();
                int newRequestsCount = 0;
                for (Contact contact : contacts) {
                    if (!(contact.invite || contact.newlyAcceptedInvite)) {
                        String letter = String.valueOf(contact.firstName.charAt(0)).toUpperCase();
                        if (!mLetter.containsKey(letter)) {
                            mLetter.put(letter, result.size());
                            result.add(new ContactAdapter.Header(letter));
                        }
                        result.add(contact);
                    } else {
                        requests.add(contact);
                        if (contact.newInvite) {
                            newRequestsCount++;
                        }
                    }
                }
                if (!requests.isEmpty()) {
                    result.addAll(0, requests);
                    result.add(0, new ContactAdapter.Header(getString(newRequestsCount > 0 ? R.string.new_requests_count : R.string.new_requests_zero, newRequestsCount),
                            newRequestsCount > 0 ? R.color.colorAccent : R.color.main_gray));
                    int addition = requests.size() + 1;
                    for (Map.Entry<String, Integer> letterPositionEntry : mLetter.entrySet()) {
                        letterPositionEntry.setValue(letterPositionEntry.getValue() + addition);
                    }
                }

                mAdapter.updateLetterMap(mLetter);
                Log.d("Con", "in back return");
                return result;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<Object>> loader, List<Object> data) {
        Log.d("Con", "Finished");
        updateData(data);
        getLoaderManager().destroyLoader(loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<List<Object>> loader) {
    }

    protected RecyclerView.Adapter setupAdapter() {
        if (mAdapter == null) {
            List<Contact> disabledContacts = Parcels.unwrap(getArguments().getParcelable(ARG_DISABLED_CONTACTS_LIST));
            List<String> disabledIds = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(disabledContacts)) {
                for (Contact contact : disabledContacts) {
                    if (!TextUtils.isEmpty(contact.phonebookId)) {
                        disabledIds.add(contact.phonebookId);
                    }
                    if (!TextUtils.isEmpty(contact.dodicallId)) {
                        disabledIds.add(contact.dodicallId);
                    }
                }
            }
            mAdapter = new ContactAdapter(getActivity(), null, disabledIds, getArguments().getBoolean(BUTTONS_CONTACTS_LIST, true), this);
        }
        return mAdapter;
    }

    @Override
    public void filterChanged(ToolBarSpinnerAdapter.BaseItem filter) {
        mCurrentFilter = filter.filterType;
        restartWithNewFilter(mCurrentFilter, null, true);
    }

    @Override
    public void invitesCounterChanged(int newCount) {
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        D.log(TAG, "[onQueryTextSubmit] query: %s", query);
        restartWithNewFilter(mCurrentFilter, query, false);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        D.log(TAG, "[onQueryTextChange] newText: %s", newText);
        restartWithNewFilter(mCurrentFilter, newText, false);
        return true;
    }

    @Override
    public void onContactSelected(Contact contact) {
        mFragmentActionListener.onOpenContact(contact);
    }

    @Override
    public void onChatWithContact(Contact contact) {
        CreateChatAsyncTask.execute(getActivity(), contact, chat -> {
            if (chat == null) {
                Toast.makeText(getActivity(), R.string.unable_create_chat, Toast.LENGTH_SHORT).show();
                return;
            }

            startActivity(ChatActivity.newIntent(getActivity(), chat));
        });
    }

    @Override
    public void onCallToContact(Contact contact) {
        mFragmentActionListener.onContactCall(contact);
    }

    @Override
    public void onAddContact(Contact contact) {
        if (!TextUtils.isEmpty(contact.phonebookId) && !contact.isSaved()) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.contact_save_confirm_msg)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        if (DialogInterface.BUTTON_POSITIVE == which) {
                            mContactsManager.saveContact(contact, ContactsManagerImpl.getPhonebookContactId(contact));
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        } else {
            ContactActionAsyncTask.getAcceptAsyncTask(mContactsManager, contact, new ContactActionAsyncTask.ContactActionListener() {
                @Override
                public void onContactActionSuccess(Contact contact) {
                    if (isAdded()) {
                        restartWithNewFilter(mCurrentFilter, null, false);
                        Snackbar.make(getView(), getString(R.string.contact_was_added), Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onContactActionFail() {
                }
            }).execute();
        }
    }

    private void restartWithNewFilter(int filter, @Nullable String query, boolean forceUpdate) {
        D.log(TAG, "[restartWithNewFilter]");
        final Bundle extra = new Bundle(2);
        extra.putInt(FILTER_TYPE, filter);
        extra.putString(FILTER_QUERY_TEXT, query);
        extra.putBoolean(FORCE_UPDATE, forceUpdate);
        getLoaderManager().destroyLoader(LOADER_ID);
        getLoaderManager().restartLoader(LOADER_ID, extra, this).forceLoad();
    }

    private void updateData(List<Object> result) {
        D.log(TAG, "[onLoadFinished]");

        mAdapter.updateData(result);
        mAdapter.notifyDataSetChanged();
        mProgress.setVisibility(View.GONE);
        if (result.isEmpty()) {
            mContent.setVisibility(View.GONE);
            mEmpty.setVisibility(View.VISIBLE);
        } else {
            mContent.setVisibility(View.VISIBLE);
            mEmpty.setVisibility(View.GONE);
        }
    }
}
