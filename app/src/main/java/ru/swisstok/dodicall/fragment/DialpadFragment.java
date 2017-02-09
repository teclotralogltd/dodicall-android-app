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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ConferenceCallActivity;
import ru.swisstok.dodicall.activity.CreateProfileActivity;
import ru.swisstok.dodicall.activity.SelectContactActivity;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.task.ContentResolverAsyncTaskLoader;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.DialpadButton;
import ru.uls_global.dodicall.BusinessLogic;

public class DialpadFragment extends BaseTabFragment {
    public static final int TAB_POSITION = 3;
    private static final int REQUEST_CODE_SELECT_CONTACT = 1;
//    private static final int LOADER_ID_GET_ME = 100;

    @BindView(R.id.text_number)
    TextView mNumberText;

    @BindView(R.id.text_contact_info)
    TextView mContactInfoText;

    @BindView(R.id.button_add_number)
    ImageButton mAddNumberButton;

    @BindView(R.id.button_all_numbers)
    ImageButton mAllNumbersButton;

    @BindView(R.id.button_erase)
    ImageButton mEraseButton;

    private static String sLastNumber;
    private int mRequestId;

//    private Contact mMe;

//    private LoaderManager.LoaderCallbacks<Contact> mMeLoaderCallbacks = new LoaderManager.LoaderCallbacks<Contact>() {
//        @Override
//        public Loader<Contact> onCreateLoader(int id, Bundle args) {
//            return new GetMeAsyncTaskLoader(getActivity());
//        }
//
//        @Override
//        public void onLoadFinished(Loader<Contact> loader, Contact data) {
//            mMe = data;
//            getActivity().invalidateOptionsMenu();
//            getLoaderManager().destroyLoader(LOADER_ID_GET_ME);
//        }
//
//        @Override
//        public void onLoaderReset(Loader<Contact> loader) {
//
//        }
//    };

    public DialpadFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getLoaderManager().initLoader(LOADER_ID_GET_ME, null, mMeLoaderCallbacks);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sLastNumber = mNumberText.getText().toString();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        return inflater.inflate(R.layout.dialpad_tab_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        if (!TextUtils.isEmpty(sLastNumber)) {
            updateNumber(sLastNumber);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (!menuVisible) {
            BusinessLogic.GetInstance().StopDtmf();
        }
    }

    //    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
//        super.onCreateOptionsMenu(menu, menuInflater);
//
//        if (mMe != null) {
////            if (CollectionUtils.isNotEmpty(mMe.phones)) {
////                for (String phone : mMe.phones) {
////                    menu.add(1, phone.hashCode(), Menu.NONE, "d-pstn " + phone);
////                }
////            }
////
////            if (CollectionUtils.isNotEmpty(mMe.sips)) {
////                for (String sip : mMe.sips) {
////                    menu.add(1, sip.hashCode(), Menu.NONE, "d-sip " + sip);
////                }
////            }
////
////            menu.setGroupCheckable(1, true, true);
////
////            if (menu.size() > 0) {
////                MenuItem first = menu.getItem(0);
////                first.setChecked(true);
////
////                getActivity().setTitle(first.getTitle());
////            }
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
////        int hashCode = item.getItemId();
//
////        if (mMe != null) {
////            if (CollectionUtils.isNotEmpty(mMe.phones)) {
////                Stream.of(mMe.phones)
////                        .filter(phone -> hashCode == phone.hashCode())
////                        .forEach((String phone) -> item.setChecked(true));
////            }
////
////            if (CollectionUtils.isNotEmpty(mMe.sips)) {
////                Stream.of(mMe.sips)
////                        .filter(sip -> hashCode == sip.hashCode())
////                        .forEach((String sip) -> item.setChecked(true));
////            }
//
////        item.setChecked(true);
////        getActivity().setTitle(item.getTitle());
//
////        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @OnClick(R.id.button_erase)
    void deleteSymbol() {
        String number = mNumberText.getText().toString();
        if (!TextUtils.isEmpty(number)) {
            updateNumber(number.substring(0, number.length() - 1));
        }
    }

    @OnLongClick(R.id.button_erase)
    boolean clearNumber() {
        updateNumber(null);
        return true;
    }

    @OnClick({
            R.id.button_one,
            R.id.button_two,
            R.id.button_three,
            R.id.button_four,
            R.id.button_five,
            R.id.button_six,
            R.id.button_seven,
            R.id.button_eight,
            R.id.button_nine,
            R.id.button_zero,
            R.id.button_star,
            R.id.button_sharp
    })
    void numberClick(View v) {
        DialpadButton button = (DialpadButton) v;
        updateNumber(String.format("%s%s", String.valueOf(mNumberText.getText()), button.getNumber()));
    }

    @OnLongClick(R.id.button_zero)
    boolean zeroLongClick() {
        updateNumber(String.format("%s%s", String.valueOf(mNumberText.getText()), "+"));
        return true;
    }

    @OnClick(R.id.button_add_number)
    void addContact() {
        String[] items = new String[]{
                getString(R.string.new_contact),
                getString(R.string.add_to_exist_contact)
        };

        new AlertDialog.Builder(getContext())
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0: {
                            CreateProfileActivity.start(getActivity(), null, mNumberText.getText().toString());
                            break;
                        }
                        case 1: {
                            startActivityForResult(SelectContactActivity.newIntent(getActivity()), REQUEST_CODE_SELECT_CONTACT);
                            break;
                        }
                    }
                })
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                Contact contact = SelectContactActivity.extractResult(data);
                CreateProfileActivity.start(getActivity(), contact, mNumberText.getText().toString());
                return;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @OnClick({R.id.button_conference, R.id.button_tariff})
    void comingSoon() {
        Utils.showComingSoon(getActivity());
    }

    @OnClick(R.id.button_call)
    void call() {
//        startActivity(new Intent(getContext(), ConferenceCallActivity.class));

        String number = mNumberText.getText().toString();
        if (TextUtils.isEmpty(number)) {
            return;
        }

        mFragmentActionListener.onCallToNumber(number);
        clearNumber();
    }

    private void updateNumber(@Nullable String number) {
        if (!isAdded()) {
            return;
        }

        Utils.setVisibility(mEraseButton, TextUtils.isEmpty(number)
                ? View.INVISIBLE
                : View.VISIBLE
        );

        mNumberText.setText(number);
        mContactInfoText.setText(null);

        Utils.setVisibility(mAddNumberButton, View.INVISIBLE);
        Utils.setVisibility(mAllNumbersButton, View.INVISIBLE);

        if (!TextUtils.isEmpty(number)) {
            String _n = number;

            int i = 0;
            for (; i < _n.length(); ++i) {
                if (_n.charAt(i) != '#') {
                    break;
                }
            }

            _n = _n.substring(i);

            if (!TextUtils.isEmpty(_n)) {
                mNumberText.postDelayed(new RetrieveContactRunnable(_n), 1000);
            }
        }
    }

    class RetrieveContactRunnable implements Runnable {
        final String mNumber;

        RetrieveContactRunnable(String number) {
            mNumber = number;
        }

        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }

            if (!TextUtils.equals(mNumber, mNumberText.getText().toString())) {
                return;
            }

            Bundle b = new Bundle(1);
            b.putString("number", mNumber);
            getLoaderManager().initLoader(++mRequestId, b, mRetrieveContactByNumberCallbacks);
        }
    }

    private LoaderManager.LoaderCallbacks<Contact> mRetrieveContactByNumberCallbacks = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            return new RetrieveContactByNumberTaskLoader(getActivity(), args.getString("number"));
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact contact) {
            if (!isAdded() || TextUtils.isEmpty(mNumberText.getText())) {
                return;
            }

            if (contact == null) {
                Utils.setVisibility(mAllNumbersButton, View.INVISIBLE);
                Utils.setVisibility(mAddNumberButton, View.VISIBLE);
                mContactInfoText.setText(getString(R.string.unknown_number));
            } else {
                Utils.setVisibility(mAllNumbersButton, View.VISIBLE);
                Utils.setVisibility(mAddNumberButton, View.INVISIBLE);
                mContactInfoText.setText(Utils.formatAccountFullName(contact));
            }

            getLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<Contact> loader) {

        }
    };

    public static class RetrieveContactByNumberTaskLoader extends ContentResolverAsyncTaskLoader<Contact> {
        private final String mNumber;

        public RetrieveContactByNumberTaskLoader(Context context, String number) {
            super(context);
            mNumber = number;
        }

        @Nullable
        @Override
        public Contact loadInBackground(@NonNull ContentResolver contentResolver) {
            Contact c = BL.retrieveContactByNumber(mNumber);

            return c.isDodicall() || c.isFromPhonebook()
                    ? c
                    : null;
        }
    }
}
