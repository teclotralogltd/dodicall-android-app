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

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.adapter.TabAdapter;
import ru.swisstok.dodicall.util.Utils;

public class BaseTabFragment extends BaseFragment {

    public BaseTabFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.base_fragment, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        final int tabPosition = getArguments().getInt(TabAdapter.TabSpec.TAB_POSITION);
        showHideActionBarItems(tabPosition, ContactsFragment.TAB_POSITION, R.id.toolbar_spinner);
    }

    protected void showHideActionBarItems(int tabPosition, int tabSpec, @IdRes int viewResId) {
        showHideActionBarItems(viewResId, tabPosition == tabSpec);
    }

    protected void showHideActionBarItems(@IdRes int viewResId, boolean show) {
        final View view = getActivity().findViewById(viewResId);
        Utils.setVisibility(view, show ? View.VISIBLE : View.GONE);
    }

}
