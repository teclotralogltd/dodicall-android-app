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

package ru.swisstok.dodicall.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.parceler.Parcels;

import java.util.ArrayList;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ChatActivity;
import ru.swisstok.dodicall.activity.OutgoingCallActivity;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.swisstok.dodicall.util.NotificationsUtils;

public class HandsUpActionsService extends IntentService {
    private static final String ACTION_WRITE = "ru.swisstok.dodicall.service.action.WRITE";
    private static final String ACTION_CALL = "ru.swisstok.dodicall.service.action.CALL";
    private static final String ACTION_PSTN_CALL = "ru.swisstok.dodicall.service.action.PSTN_CALL";
    private static final String ACTION_DECLINE_INVITATION = "ru.swisstok.dodicall.service.action.ACTION_DECLINE_INVITATION";
    private static final String ACTION_ACCEPT_INVITATION = "ru.swisstok.dodicall.service.action.ACTION_ACCEPT_INVITATION";

    private static final String EXTRA_CONTACT = "ru.swisstok.dodicall.service.extra.CONTACT";
    private static final String EXTRA_NUMBER = "ru.swisstok.dodicall.service.extra.NUMBER";

    public static Intent newIntentForWrite(Context context, Contact contact) {
        Intent i = new Intent(context, HandsUpActionsService.class);
        i.setAction(ACTION_WRITE);
        i.putExtra(EXTRA_CONTACT, Parcels.wrap(contact));

        return i;
    }

    public static Intent newIntentForCall(Context context, Contact contact) {
        Intent i = new Intent(context, HandsUpActionsService.class);
        i.setAction(ACTION_CALL);
        i.putExtra(EXTRA_CONTACT, Parcels.wrap(contact));

        return i;
    }

    public static Intent newIntentForPstnCall(Context context, String number) {
        Intent i = new Intent(context, HandsUpActionsService.class);
        i.setAction(ACTION_PSTN_CALL);
        i.putExtra(EXTRA_NUMBER, number);

        return i;
    }

    public static Intent newIntentForDeclineInvitation(Context context, Contact contact) {
        Intent i = new Intent(context, HandsUpActionsService.class);
        i.setAction(ACTION_DECLINE_INVITATION);
        i.putExtra(EXTRA_CONTACT, Parcels.wrap(contact));

        return i;
    }

    public static Intent newIntentForAcceptInvitation(Context context, Contact contact) {
        Intent i = new Intent(context, HandsUpActionsService.class);
        i.setAction(ACTION_ACCEPT_INVITATION);
        i.putExtra(EXTRA_CONTACT, Parcels.wrap(contact));

        return i;
    }

    public HandsUpActionsService() {
        super("HandsUpActionsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            NotificationsUtils.cancelNotification(this);
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            final String action = intent.getAction();
            if (ACTION_WRITE.equals(action)) {
                handleActionWrite(Parcels.unwrap(intent.getParcelableExtra(EXTRA_CONTACT)));
            } else if (ACTION_CALL.equals(action)) {
                handleActionCall(Parcels.unwrap(intent.getParcelableExtra(EXTRA_CONTACT)));
            } else if (ACTION_PSTN_CALL.equals(action)) {
                handleActionPstnCall(intent.getStringExtra(EXTRA_NUMBER));
            } else if (ACTION_DECLINE_INVITATION.equals(action)) {
                handleActionDeclineInvitation(Parcels.unwrap(intent.getParcelableExtra(EXTRA_CONTACT)));
            } else if (ACTION_ACCEPT_INVITATION.equals(action)) {
                handleActionAcceptInvitation(Parcels.unwrap(intent.getParcelableExtra(EXTRA_CONTACT)));
            }
        }
    }

    private void handleActionDeclineInvitation(Contact contact) {
        ContentResolver cr = getContentResolver();
        if (cr == null) {
            return;
        }

        int result = cr.delete(contact.getUri(), String.valueOf(DataProvider.FILTER_SUBSCRIPTIONS), null);
        Log.d("aaa", "decline invitation result: " + result);
    }

    private void handleActionAcceptInvitation(Contact contact) {
        ContentResolver cr = getContentResolver();
        if (cr == null) {
            return;
        }

        Uri uri = cr.insert(
                ContentUris.withAppendedId(DataProvider.CONTACTS_URI, Math.abs(DataProvider.INVITE_MAGIC_ID + contact.dodicallId.hashCode())),
                contact.toContentValues());

        Log.d("aaa", "accept invitation result: " + uri);
    }

    private void handleActionWrite(Contact contact) {
        ArrayList<Contact> list = new ArrayList<>(1);
        list.add(contact);

        CreateChatAsyncTask task = new CreateChatAsyncTask(this, list, chat -> {
        });

        Chat chat = task.loadInBackground(getContentResolver());

        if (chat != null) {
            startActivity(ChatActivity.newIntent(this, chat, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } else {
            Toast.makeText(this, R.string.toast_unable_create_chat, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleActionCall(Contact contact) {
        startActivity(OutgoingCallActivity.newIntent(this, contact).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    private void handleActionPstnCall(String number) {
        startActivity(OutgoingCallActivity.newIntent(this, number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
