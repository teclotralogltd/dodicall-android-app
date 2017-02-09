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
import android.graphics.Typeface;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.api.ContactStatus;
import ru.swisstok.dodicall.util.Utils;
import ru.swisstok.dodicall.view.RoundedImageView;

public class ContactAdapter extends BaseAdapter<Object, RecyclerView.ViewHolder> {

    private static final int CHAR_HEADER_TYPE = 0;
    private static final int CONTACT_TYPE = 1;

    private boolean mWithButtons;
    private Map<String, Integer> mLetterPositionMap;
    private ContactActionListener mContactActionListener;
    private List<String> mDisabledContacts;

    public ContactAdapter(Context context, List<Object> data, List<String> disabledContacts, boolean withButtons, ContactActionListener contactActionListener) {
        super(context, data);
        mLetterPositionMap = new HashMap<>();
        mDisabledContacts = disabledContacts;
        mWithButtons = withButtons;
        mContactActionListener = contactActionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) instanceof Contact ? CONTACT_TYPE : CHAR_HEADER_TYPE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == CHAR_HEADER_TYPE ?
                new CharViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.contact_list_char_header, parent, false)) :
                new ContactViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.contact_row, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        if (itemViewType == CHAR_HEADER_TYPE) {
            Header header = (Header) getItem(position);
            TextView headerText = ((TextView) holder.itemView);
            headerText.setText(header.mText);
            headerText.setTextColor(ContextCompat.getColor(getContext(), header.mColor));
        } else if (itemViewType == CONTACT_TYPE) {
            Contact contact = (Contact) getItem(position);
            ContactViewHolder contactHolder = ((ContactViewHolder) holder);

            Picasso.with(getContext())
                    .load(new File(contact.avatarPath))
                    .networkPolicy(NetworkPolicy.NO_STORE)
                    .transform(ROUNDED_TRANSFORMATION)
                    .placeholder(R.drawable.no_photo_user)
                    .into(contactHolder.mRoundedImageView);

            contactHolder.mDodicallContact.setVisibility(TextUtils.isEmpty(contact.dodicallId) ? View.GONE : View.VISIBLE);

            contactHolder.mContactName.setText(Utils.formatAccountFullName(contact));
            if (TextUtils.isEmpty(contact.dodicallId)) {
                contactHolder.mContactStatus.setVisibility(View.GONE);
                contactHolder.mCallView.setImageResource(R.drawable.phone_pstn);
            } else {
                contactHolder.mContactStatus.setVisibility(View.VISIBLE);
                if (!contact.invite && !contact.subscriptionRequest) {
                    contactHolder.mContactStatus.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
                    StatusesAdapter.setupStatusView(contactHolder.mContactStatus, contact.getStatus(), contact.getExtraStatus());
                } else {
                    contactHolder.mContactStatus.getCompoundDrawables()[0].setLevel(ContactStatus.STATUS_NOT_FRIEND);
                    contactHolder.mContactStatus.setText(contact.subscriptionRequest ? R.string.contact_request_sent : R.string.contact_waiting_for_acceptance);
                    contactHolder.mContactStatus.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                }
                contactHolder.mCallView.setImageResource(R.drawable.contacts_list_item_call_ic);
            }

            int imageLevel = StatusesAdapter.getStatusDrawableLevel(contact.getStatus());
            contactHolder.mCallView.setImageLevel(imageLevel);
            contactHolder.mChatView.setImageLevel(imageLevel);

            contactHolder.mCallView.setOnClickListener(view -> mContactActionListener.onCallToContact(contact));

            contactHolder.mChatView.setVisibility((!contact.invite && !contact.subscriptionRequest) && !TextUtils.isEmpty(contact.dodicallId) ? View.VISIBLE : View.GONE);
            contactHolder.mChatView.setOnClickListener(view -> mContactActionListener.onChatWithContact(contact));

            contactHolder.mAddToContactsView.setVisibility(contact.invite || (!TextUtils.isEmpty(contact.phonebookId) && !contact.isSaved()) ? View.VISIBLE : View.GONE);
            contactHolder.mAddToContactsView.setImageResource(contact.invite ? R.drawable.accept_user_request : R.drawable.ic_add_to_saved);
            contactHolder.mAddToContactsView.setOnClickListener(view -> mContactActionListener.onAddContact(contact));

            contactHolder.mButtonsContainer.setVisibility(mWithButtons ? View.VISIBLE : View.GONE);
            if (isContactEnabled(contact)) {
                contactHolder.itemView.setAlpha(1);
                contactHolder.itemView.setOnClickListener(view -> mContactActionListener.onContactSelected(contact));
            } else {
                contactHolder.itemView.setAlpha(0.3f);
            }
        }
    }

    private boolean isContactEnabled(Contact contact) {
        return !(mDisabledContacts.contains(contact.dodicallId) || mDisabledContacts.contains(contact.phonebookId));
    }

    public void updateLetterMap(Map<String, Integer> letterPositionMap) {
        mLetterPositionMap = letterPositionMap;
    }

    public int getSectionPositionForLetter(String letter) {
        if (mLetterPositionMap.containsKey(letter)) {
            return mLetterPositionMap.get(letter);
        }
        return INVALID_POSITION;
    }

    private static class CharViewHolder extends RecyclerView.ViewHolder {
        private CharViewHolder(View itemView) {
            super(itemView);
        }
    }

    protected static class ContactViewHolder extends RecyclerView.ViewHolder {

        public RoundedImageView mRoundedImageView;
        public View mDodicallContact;
        public TextView mContactName;
        public TextView mContactStatus;

        public View mButtonsContainer;
        public ImageView mCallView;
        public ImageView mChatView;
        public ImageView mAddToContactsView;

        private ContactViewHolder(View itemView) {
            super(itemView);
            mRoundedImageView = (RoundedImageView) itemView.findViewById(R.id.contact_avatar);
            mDodicallContact = itemView.findViewById(R.id.contact_is_dodicall);
            mContactName = (TextView) itemView.findViewById(R.id.contact_name);
            mContactStatus = (TextView) itemView.findViewById(R.id.contact_status);
            mButtonsContainer = itemView.findViewById(R.id.buttons_container);
            mCallView = (ImageView) itemView.findViewById(R.id.call_button);
            mChatView = (ImageView) itemView.findViewById(R.id.chat_button);
            mAddToContactsView = (ImageView) itemView.findViewById(R.id.add_to_contacts_button);
        }
    }

    public static class Header {
        private String mText;
        @ColorRes
        private int mColor;

        public Header(String text) {
            this(text, R.color.main_gray);
        }

        public Header(String text, @ColorRes int color) {
            mText = text;
            mColor = color;
        }
    }

    public interface ContactActionListener {
        void onContactSelected(Contact contact);

        void onChatWithContact(Contact contact);

        void onCallToContact(Contact contact);

        void onAddContact(Contact contact);
    }
}
