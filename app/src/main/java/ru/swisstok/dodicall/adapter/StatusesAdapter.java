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

package ru.swisstok.dodicall.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.List;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.util.D;

public class StatusesAdapter extends ArrayAdapter<Integer> {

    private static final String TAG = "StatusesAdapter";

    private int mSelectedStatus;

    public StatusesAdapter(Context context, int resource, List<Integer> objects) {
        super(context, resource, objects);
    }

    public StatusesAdapter(Context context, int resource, Integer[] objects, int selectedStatus) {
        super(context, resource, objects);
        mSelectedStatus = selectedStatus;
    }

    public void setSelectedStatus(int selectedStatus) {
        mSelectedStatus = selectedStatus;
    }

    public int getSelectedStatus() {
        return mSelectedStatus;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.status_list_item, parent, false);
        }
        Integer status = getItem(position);
        TextView statusTextView = (TextView) view.findViewById(R.id.status_text);
        setupStatusView(statusTextView, status);

        RadioButton mSelectedStatusBox = (RadioButton) view.findViewById(R.id.status_selection_rb);
        mSelectedStatusBox.setChecked(status == mSelectedStatus);
        return view;
    }

    public static void setupStatusView(TextView view, int status) {
        setupStatusView(view, status, null);
    }

    private static
    @StringRes
    int getStatusText(int status) {
        if (status == ContactStatus.STATUS_OFFLINE) {
            return R.string.base_user_status_offline;
        } else if (status == ContactStatus.STATUS_ONLINE) {
            return R.string.base_user_status_online;
        } else if (status == ContactStatus.STATUS_HIDDEN) {
            return R.string.base_user_status_hidden;
        } else if (status == ContactStatus.STATUS_AWAY) {
            return R.string.base_user_status_away;
        } else if (status == ContactStatus.STATUS_DND) {
            return R.string.base_user_status_dnd;
        }
        return R.string.base_user_status_offline;
    }

    public static int getStatusDrawableLevel(int status) {
        if (status == ContactStatus.STATUS_OFFLINE) {
            return 1;
        } else if (status == ContactStatus.STATUS_ONLINE) {
            return 3;
        } else if (status == ContactStatus.STATUS_HIDDEN) {
            return 1;
        } else if (status == ContactStatus.STATUS_AWAY) {
            return 2;
        } else if (status == ContactStatus.STATUS_DND) {
            return 2;
        }
        return 1;
    }

    public static void setupStatusView(TextView view, int status, @Nullable String extraStatus) {
        D.log(TAG, "[setupStatusView] extraStatus: %s", extraStatus);

        String statusText = view.getResources().getString(getStatusText(status));

        if (!TextUtils.isEmpty(extraStatus)) {
            view.setText(String.format("%s. %s", statusText, extraStatus));
        } else {
            view.setText(statusText);
        }

        view.getCompoundDrawables()[0].setLevel(getStatusDrawableLevel(status));
    }
}
