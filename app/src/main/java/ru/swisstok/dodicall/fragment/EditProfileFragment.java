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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.parceler.Parcels;

import java.util.ArrayList;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.manager.impl.ContactsManagerImpl;
import ru.swisstok.dodicall.util.D;

public class EditProfileFragment extends Fragment {

    public interface OnFragmentInteractionListener {
        void onContactSaved(Contact contact);
    }

    public static final String CONTACT = "contact_arg";
    public static final String ARG_NUMBER = "argNumber";

    public static final String TAG = "EditProfileFragment";
    private LinearLayout mExtraContactsListWrapper;
    private ScrollView mScrollWrapper;
    private EditText mFirstName;
    private EditText mLastName;
    private Contact mContact;
    private String mExtraNumber;
    private OnFragmentInteractionListener mFragmentInteraction;

    public EditProfileFragment() {
    }

    public static EditProfileFragment getInstance(@Nullable Contact contact, String extraNumber) {
        Bundle args = new Bundle();
        if (contact != null) {
            args.putParcelable(CONTACT, Parcels.wrap(contact));
        }
        args.putParcelable(ARG_NUMBER, Parcels.wrap(extraNumber));

        EditProfileFragment fragment = new EditProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mFragmentInteraction = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement EditProfileFragment.OnFragmentInteractionListener");
        }

        if (getArguments() != null) {
            mContact = Parcels.unwrap(getArguments().getParcelable(CONTACT));
            mExtraNumber = Parcels.unwrap(getArguments().getParcelable(ARG_NUMBER));
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        D.log(TAG, "[onCreateView]");
        View view = inflater.inflate(R.layout.profile_edit, container, false);
        view.findViewById(R.id.add_extra_contact).setOnClickListener(v -> addExtraContact());
        mExtraContactsListWrapper = (LinearLayout) view.findViewById(
                R.id.extra_contacts_list_wrapper
        );
        mFirstName = (EditText) view.findViewById(R.id.contact_firstname);
        mLastName = (EditText) view.findViewById(R.id.contact_lastname);
        mScrollWrapper = (ScrollView) getActivity().findViewById(R.id.scroll_wrapper);
        loadContact(mContact);
        return view;
    }

    private void loadContact(@Nullable Contact contact) {
        D.log(TAG, "[loadContact] contact: %s", contact);
        if (contact != null) {
            mFirstName.setText(contact.firstName);
            mLastName.setText(contact.lastName);
            if (!TextUtils.isEmpty(contact.dodicallId)) {
                mFirstName.setEnabled(false);
                mLastName.setEnabled(false);
                mFirstName.setFocusable(false);
                mLastName.setFocusable(false);
            }
            for (String phone : contact.phones) {
                D.log(TAG, "[loadContact] phone: %s; empty: %s;", phone, TextUtils.isEmpty(phone));
                if (!TextUtils.isEmpty(phone)) {
                    addExtraContact(phone);
                }
            }
        }

        if (!TextUtils.isEmpty(mExtraNumber)) {
            addExtraContact(mExtraNumber);

            if (contact == null) {
                mFirstName.postDelayed(() -> mFirstName.requestFocus(), 100);
            }
        }

        addExtraContact();
    }

    private void addExtraContact() {
        addExtraContact(null);
    }

    private void addExtraContact(@Nullable String phoneNumber) {
        final View extraContact = LayoutInflater.from(getContext()).inflate(
                R.layout.profile_edit_extra_contact, mExtraContactsListWrapper, false
        );
        mExtraContactsListWrapper.addView(extraContact);
        int index = mExtraContactsListWrapper.indexOfChild(extraContact);
        int childCount = mExtraContactsListWrapper.getChildCount();
        ImageButton deleteContactButton =
                (ImageButton) extraContact.findViewById(R.id.delete);
        deleteContactButton.setOnClickListener(v -> {
            mExtraContactsListWrapper.removeView(extraContact);
            View child = mExtraContactsListWrapper.getChildAt(
                    mExtraContactsListWrapper.getChildCount() - 1
            );
            if (child != null) {
                //TODO: how to refresh if it have a focus?
                ((EditText) child.findViewById(
                        R.id.phone_number
                )).setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
        });
        D.log(TAG, "[addExtraContact] index: %d; childCount: %d;", index, childCount);
        final EditText phoneNumberEditText = ((EditText) extraContact.findViewById(
                R.id.phone_number
        ));
        if (!TextUtils.isEmpty(phoneNumber)) {
            phoneNumberEditText.setText(phoneNumber);
        }
        phoneNumberEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        phoneNumberEditText.setOnEditorActionListener((v, actionId, event) -> {
            D.log(TAG, "[onEditorAction] actionId: %d", actionId);
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveContact();
            }
            return false;
        });
        if (childCount > 1) {
            //setup previous input
            ((EditText) mExtraContactsListWrapper.getChildAt(index - 1).findViewById(
                    R.id.phone_number
            )).setImeOptions(EditorInfo.IME_ACTION_NEXT);
            //scroll to bottom
            mScrollWrapper.post(() -> {
                mScrollWrapper.fullScroll(View.FOCUS_DOWN);
                phoneNumberEditText.requestFocus();
            });
        }
    }

    public Contact getContact() {
        Contact contact = mContact == null ? new Contact() : mContact;
        contact.firstName = mFirstName.getText().toString();
        contact.lastName = mLastName.getText().toString();
        ArrayList<String> phones = new ArrayList<>();
        String phone;
        for (int i = 0; i < mExtraContactsListWrapper.getChildCount(); i++) {
            phone = ((EditText) mExtraContactsListWrapper.getChildAt(i).findViewById(
                    R.id.phone_number
            )).getText().toString();
            if (!stringIsEmpty(phone)) {
                phones.add(phone.trim());
            }
        }
        contact.phones = phones;
        return contact;
    }

    public static boolean stringIsEmpty(String number) {
        return TextUtils.isEmpty(number) || number.matches("^\\s*$");
    }


    public void saveContact() {
        saveContact(getContact());
    }

    private void saveContact(Contact contact) {
        mFirstName.setError(null);
        mLastName.setError(null);

        if (TextUtils.isEmpty(mFirstName.getText())) {
            mFirstName.requestFocus();
            mFirstName.setError(getString(R.string.error_first_name_empty));
            return;
        }

        if (TextUtils.isEmpty(mLastName.getText())) {
            mLastName.requestFocus();
            mLastName.setError(getString(R.string.error_last_name_empty));
            return;
        }

        mFragmentInteraction.onContactSaved(ContactsManagerImpl.getInstance().saveContact(contact, contact.getId()));
    }
}
