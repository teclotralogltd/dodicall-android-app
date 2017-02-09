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
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.SettingsActivity;
import ru.swisstok.dodicall.preference.MaterialEditTextPreferenceDialogFragment;
import ru.swisstok.dodicall.preference.MaterialListPreferenceDialogFragment;
import ru.swisstok.dodicall.preference.PreferenceDividerDecoration;
import ru.swisstok.dodicall.util.D;

public abstract class BasePreferenceFragment extends PreferenceFragment {

    private static final String TAG = "BasePreferenceFragment";
    private static final String DIALOG_FRAGMENT_TAG =
            "android.support.v14.preference.PreferenceFragment.DIALOG";

    public static final String PREF_TITLE = "PREF_TITLE";

    private CharSequence mPreviousTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.white));
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().addItemDecoration(
                new PreferenceDividerDecoration(
                        getActivity(), R.drawable.preferences_divider, R.dimen.divider_height
                ).drawBottom(true)
        );
        setDivider(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getId() != R.id.headers_content) {
            initTitle(((SettingsActivity) getActivity()).getSupportActionBar());
        }
    }

    private void initTitle(@Nullable ActionBar actionBar) {
        if (actionBar != null) {
            mPreviousTitle = actionBar.getTitle();
            actionBar.setTitle(getTitle());
        }
    }

    private String getTitle() {
        if (getArguments() != null) {
            D.log(TAG, "[getTitle] get title from args: %s", getArguments().getString(PREF_TITLE));
            return getArguments().getString(PREF_TITLE);
        } else {
            D.log(TAG, "[getTitle] get title from activity extras: %s", getActivity().getIntent().getExtras().getString(PREF_TITLE));
            return getActivity().getIntent().getExtras().getString(PREF_TITLE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (getId() != R.id.headers_content) {
            final ActionBar actionBar = ((SettingsActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(mPreviousTitle);
            }
        }
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }
        final DialogFragment f;
        if (preference instanceof EditTextPreference) {
            f = MaterialEditTextPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof ListPreference) {
            f = MaterialListPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            throw new IllegalArgumentException(
                    "Tried to display dialog for unknown preference type. Did you forget to override onDisplayPreferenceDialog()?"
            );
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

}
