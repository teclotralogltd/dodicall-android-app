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
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.util.CollectionUtils;
import ru.swisstok.dodicall.util.Utils;

public abstract class BaseListFragment<DATA, HOLDER extends RecyclerView.ViewHolder> extends Fragment {
    @BindView(R.id.no_data_text)
    TextView mNoDataText;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private List<DATA> mData = new ArrayList<>(0);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mRecyclerView.setAdapter(new Adapter());
    }

    public void setListData(List<DATA> data) {
        Utils.setVisibility(mProgress, View.GONE);

        if (CollectionUtils.isEmpty(data)) {
            Utils.setVisibility(mNoDataText, View.VISIBLE);
            Utils.setVisibility(mRecyclerView, View.GONE);
        } else {
            Utils.setVisibility(mNoDataText, View.GONE);
            Utils.setVisibility(mRecyclerView, View.VISIBLE);

            mData = data;
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    class Adapter extends RecyclerView.Adapter<HOLDER> {
        @Override
        public HOLDER onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = getActivity().getLayoutInflater().inflate(getItemLayoutId(viewType), parent, false);
            return onCreateHolder(view);
        }

        @Override
        public void onBindViewHolder(HOLDER holder, int position) {
            final DATA data = mData.get(position);
            onBindHolder(holder, data);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    @LayoutRes
    public abstract int getItemLayoutId(int viewType);

    public abstract HOLDER onCreateHolder(@NonNull View view);

    public abstract void onBindHolder(@NonNull HOLDER holder, @NonNull DATA value);

}
