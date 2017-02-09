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

import android.support.v4.app.Fragment;

import ru.swisstok.dodicall.api.CallHistory;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;

public class BaseFragment extends Fragment {

    protected FragmentActionListener mFragmentActionListener;

    public void setFragmentActionListener(FragmentActionListener fragmentActionListener) {
        mFragmentActionListener = fragmentActionListener;
    }

    public interface FragmentActionListener {
        void onContactCall(Contact contact);

        void onOpenContact(Contact contact);

        void onCallToNumber(String number);

        void onOpenHistory(CallHistory callHistory);

        void onOpenChat(Chat chat);
    }
}
