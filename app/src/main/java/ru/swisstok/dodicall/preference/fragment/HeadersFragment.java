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

import android.app.ListFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.SettingsActivity;
import ru.swisstok.dodicall.util.D;

public class HeadersFragment extends ListFragment implements AdapterView.OnItemClickListener {

    public static final String TAG = "HeadersFragment";
    private static final String HEADERS_LIST = "HEADERS_LIST";
    private static final String INIT_HEADER = "INIT_HEADER";


    public interface OnHeaderClickListener {
        void onHeaderClick(Header header);
    }

    @Parcel
    public static class Header {

        public String title;
        public Class<? extends BasePreferenceFragment> fragmentClass;

        public Header() {
        }

        public Header(String title, Class<? extends BasePreferenceFragment> fragmentClass) {
            this.title = title;
            this.fragmentClass = fragmentClass;
        }

        @Override
        public String toString() {
            return title;
        }

    }

    public HeadersFragment() {
    }

    public static HeadersFragment newInstance(List<Header> headers, String initHeader) {
        final HeadersFragment f = new HeadersFragment();
        final Bundle args = new Bundle(2);
        args.putParcelable(HEADERS_LIST, Parcels.wrap(headers));
        args.putString(INIT_HEADER, initHeader);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        D.log(TAG, "[onActivityCreated]");
        super.onActivityCreated(savedInstanceState);
        final ArrayList<Header> headers = Parcels.unwrap(
                getArguments().getParcelable(HEADERS_LIST)
        );
        setListAdapter(new ArrayAdapter<>(
                getActivity(), R.layout.preference_header, headers
        ));
        final String initHeader = getArguments().getString(INIT_HEADER);
        if (TextUtils.isEmpty(initHeader)) {
            if (SettingsActivity.isXLargeTablet()) {
                openHeader(0);
            }
        } else {
            for (int i = 0; i < headers.size(); i++) {
                if (TextUtils.equals(headers.get(i).fragmentClass.getName(), initHeader)) {
                    openHeader(i);
                }
            }
        }
    }

    public void openHeader(int headerPosition) {
        getListView().performItemClick(
                getListAdapter().getView(headerPosition, null, null),
                headerPosition, headerPosition
        );
        getListView().requestFocusFromTouch();
        getListView().setSelection(headerPosition);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_headers, parent, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (SettingsActivity.isXLargeTablet()) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        }
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        D.log(TAG, "[onItemClick]");
        if (getActivity() instanceof OnHeaderClickListener) {
            ((OnHeaderClickListener) getActivity()).onHeaderClick(
                    ((Header) getListAdapter().getItem(position))
            );
        }
    }

}
