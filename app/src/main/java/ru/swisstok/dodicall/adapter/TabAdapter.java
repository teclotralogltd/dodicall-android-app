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
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;

import java.util.List;

import ru.swisstok.dodicall.fragment.BaseFragment;
import ru.swisstok.dodicall.util.NotificationsUtils;
import ru.swisstok.dodicall.view.BadgeView;

public class TabAdapter extends FragmentStatePagerAdapter {

    private List<TabSpec> mTabSpecs;
    private Context mContext;

    private BaseFragment.FragmentActionListener mFragmentActionListener;

    SparseArray<Fragment> mFragments = new SparseArray<>();

    public TabAdapter(FragmentManager fm, Context context, List<TabSpec> specs, BaseFragment.FragmentActionListener fragmentActionListener) {
        super(fm);
        mTabSpecs = specs;
        mContext = context;
        mFragmentActionListener = fragmentActionListener;
    }

    @Override
    public Fragment getItem(int position) {
        Fragment fragment = mFragments.get(position);
        if (fragment == null) {
            final TabSpec spec = mTabSpecs.get(position);
            fragment = Fragment.instantiate(mContext, spec.tClass.getName(), spec.args);
        }
        if (fragment instanceof BaseFragment) {
            ((BaseFragment) fragment).setFragmentActionListener(mFragmentActionListener);
        }
        return fragment;
    }

    @Override
    public int getCount() {
        return mTabSpecs.size();
    }

    public static final class TabSpec {

        public static final String TAB_POSITION = "tab_position";

        public Class<? extends Fragment> tClass;
        public int title;
        public int icon;
        public Bundle args;
        public NotificationsUtils.NotificationType notificationType;

        private BadgeView mCounter;

        public TabSpec(Class<? extends Fragment> tClass, @StringRes int title, @DrawableRes int icon, Bundle args, int pos, NotificationsUtils.NotificationType notificationType) {
            this.tClass = tClass;
            this.title = title;
            this.icon = icon;
            this.notificationType = notificationType;
            if (args == null) {
                args = new Bundle();
            }
            args.putInt(TAB_POSITION, pos);
            this.args = args;
        }

        public void setCounter(BadgeView counter) {
            this.mCounter = counter;
        }

        public BadgeView getCounter() {
            return mCounter;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";//mContext.getResources().getString(mTabSpecs.get(position).title);
    }

}
