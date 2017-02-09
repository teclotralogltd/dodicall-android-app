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

package ru.swisstok.dodicall.preference.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

import ru.swisstok.dodicall.BuildConfig;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.EditChatSecurityKeyActivity;
import ru.swisstok.dodicall.activity.SettingsActivity;
import ru.swisstok.dodicall.fragment.ChatSecurityBottomSheetFragment;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.util.StorageUtils;
import ru.uls_global.dodicall.BusinessLogic;

public class ChatSecurityPreferenceFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    private static final int MAX_CLICK_COUNT = 5;
    private static final String REMOVE_USER_KEY_PREFERENCE = "pref_remove_key";

    private int mClickCount;
    private ChatSecurityBottomSheetFragment mChatSecurityBottomSheetFragment;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (BuildConfig.DEBUG) {
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setOnClickListener(v -> {
                    mClickCount++;
                    if (mClickCount == MAX_CLICK_COUNT) {
                        if (findPreference(REMOVE_USER_KEY_PREFERENCE) == null) {
                            Preference preference = new Preference(getActivity());
                            preference.setKey(REMOVE_USER_KEY_PREFERENCE);
                            preference.setTitle(R.string.pref_chats_remove_key_title);
                            preference.setOnPreferenceClickListener(preference1 -> {
                                try {
                                    StorageUtils.storeChatKey(getActivity(), new char[]{}, BusinessLogic.GetInstance().GetGlobalApplicationSettings().getLastLogin());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Toast.makeText(getActivity(), R.string.user_key_removed_message, Toast.LENGTH_SHORT).show();
                                return true;
                            });
                            getPreferenceScreen().addPreference(preference);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(null);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_chat_security);

        findPreference(Preferences.Fields.PREF_CHAT_EXPORT_KEY).setOnPreferenceClickListener(this);
        findPreference(Preferences.Fields.PREF_CHAT_IMPORT_KEY).setOnPreferenceClickListener(this);

        mChatSecurityBottomSheetFragment = new ChatSecurityBottomSheetFragment();
        mChatSecurityBottomSheetFragment.setupKey(getActivity());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit_text_layout, null, false);
        TextInputLayout passwordInput = (TextInputLayout) view.findViewById(R.id.dialog_input);
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).
                setTitle(R.string.password_title).
                setView(view).
                setNegativeButton(android.R.string.cancel, null).
                setPositiveButton(android.R.string.ok, (dialog, which) -> {
                }).
                create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getEditText().getText().toString();
            if (!TextUtils.isEmpty(password)) {
                try {
                    char[] storedPassword = StorageUtils.getPassword(getActivity());
                    if (Arrays.equals(password.toCharArray(), (storedPassword))) {
                        openSelectedOption(preference.getKey().equals(Preferences.Fields.PREF_CHAT_EXPORT_KEY));
                        alertDialog.dismiss();
                    } else {
                        passwordInput.setError(getString(R.string.password_wrong_error));
                    }
                    Arrays.fill(storedPassword, '0');
                    passwordInput.getEditText().setText(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                passwordInput.setError(getString(R.string.password_empty_error));
            }
        });
        alertDialog.show();
        return true;
    }

    private void openSelectedOption(boolean isExport) {
        if (isExport) {
            mChatSecurityBottomSheetFragment.show(((SettingsActivity) getActivity()).getSupportFragmentManager(), mChatSecurityBottomSheetFragment.getTag());
        } else {
            startActivity(new Intent(getActivity(), EditChatSecurityKeyActivity.class));
        }
    }
}
