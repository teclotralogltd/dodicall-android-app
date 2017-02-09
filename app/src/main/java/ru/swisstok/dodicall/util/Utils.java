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

package ru.swisstok.dodicall.util;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.annimon.stream.function.Function;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.swisstok.dodicall.R;
import ru.swisstok.dodicall.activity.ChatActivity;
import ru.swisstok.dodicall.adapter.StatusesAdapter;
import ru.swisstok.dodicall.api.Balance;
import ru.swisstok.dodicall.api.Call;
import ru.swisstok.dodicall.api.Chat;
import ru.swisstok.dodicall.api.ChatMessage;
import ru.swisstok.dodicall.api.ChatNotificationData;
import ru.swisstok.dodicall.api.Contact;
import ru.swisstok.dodicall.bl.BL;
import ru.swisstok.dodicall.fragment.BaseFragment;
import ru.swisstok.dodicall.preference.Preferences;
import ru.swisstok.dodicall.provider.DataProvider;
import ru.swisstok.dodicall.task.CreateChatAsyncTask;
import ru.uls_global.dodicall.BusinessLogic;
import ru.uls_global.dodicall.ServerAreaModelWrapper;
import ru.uls_global.dodicall.ServerAreasList;
import ru.uls_global.dodicall.UserSettingsModel;

public class Utils {

    private static final String TAG = "Utils";

    public static final String EXTRA_FROM_PUSH = "extra_from_push";
    public static final String EXTRA_CHAR_ID = "extra_chat_id";

    private static DateFormat sThisYearFormat;
    private static DateFormat sPreviousYearFormat;
    private static DateFormat sTimeFormat;

    public static int dipToPixels(int dip) {
        return ((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dip,
                Resources.getSystem().getDisplayMetrics()
        ));
    }

    //TODO: return struct, don't print
    public static void showScreenInfo(WindowManager wm) {
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int dens = metrics.densityDpi;
        double wi = (double) width / (double) dens;
        double hi = (double) height / (double) dens;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        double screenInches = Math.sqrt(x + y);
        D.log(TAG, "[showScreenInfo] density: %d; width: %d; height: %d; inches: %f", dens, width, height, screenInches);
    }

    public static void switchLanguage(Context context, String lang, boolean beforeLogin) {
        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        D.log(TAG, "[switchLanguage] lang: %s; current: %s;", lang, conf.locale.toString());
        conf.locale = new Locale(lang.toLowerCase());
        res.updateConfiguration(conf, res.getDisplayMetrics());
        /*Preferences.get(context).edit().putString(
                Preferences.Fields.PREF_INTERFACE_LANGUAGE, lang
        ).commit();*/
        if (beforeLogin) {
            BusinessLogic.GetInstance().SaveDefaultGuiLanguage(lang);
        } else {
            UserSettingsModel userSettings = BusinessLogic.GetInstance().GetUserSettings();
            userSettings.setGuiLanguage(lang);
            BusinessLogic.GetInstance().SaveUserSettings(userSettings);
        }
        sTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale(lang));
        sThisYearFormat = new SimpleDateFormat("dd MMM", new Locale(lang));
        sPreviousYearFormat = new SimpleDateFormat("MM.yyyy", new Locale(lang));
    }

    public static void switchLanguage(Context context, String lang) {
        switchLanguage(context, lang, false);
    }

    public static String getLocale(Context context) {
        return getLocale(context, false);
    }

    public static String getLocale(Context context, boolean init) {
        String locale;
        if (!init) {
            locale = BusinessLogic.GetInstance().GetUserSettings().getGuiLanguage();
        } else {
            locale = BusinessLogic.GetInstance().GetGlobalApplicationSettings()
                    .getDefaultGuiLanguage();
        }
        if (TextUtils.isEmpty(locale)) {
            D.log(TAG, "[getLocale] locale not found");
            //TODO: get system language
            if (context.getResources().getConfiguration().locale.toString().startsWith("ru")) {
                locale = context.getString(R.string.pref_interface_language_ru_value);
            } else if (context.getResources().getConfiguration().locale.toString().startsWith("en")) {
                locale = context.getString(R.string.pref_interface_language_en_value);
            } else if (context.getResources().getConfiguration().locale.toString().startsWith("tr")) {
                locale = context.getString(R.string.pref_interface_language_tr_value);
            }
        }
        return locale;
    }

    public static void copyTextToClipboard(Context context, String text, @StringRes int toastMessageResId) {
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Text", text));
        Toast.makeText(context.getApplicationContext(), toastMessageResId, Toast.LENGTH_SHORT).show();
    }

    public static void showComingSoon(Context context) {
        Toast.makeText(
                context.getApplicationContext(), "Coming soon...", Toast.LENGTH_SHORT
        ).show();
    }

    public static Object[] concat(Object[] a, Object[] b) {
        Object[] c = new Object[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static String getBalanceText(Balance balance) {
        String currencyValue = "UNKNOWN";
        if (balance.currency == Balance.CURRENCY_RUBLE) {
            currencyValue = "\u20bd";
        } else if (balance.currency == Balance.CURRENCY_USD) {
            currencyValue = "$";
        } else if (balance.currency == Balance.CURRENCY_EURO) {
            currencyValue = "\u20ac";
        }
        D.log(TAG, "[getBalanceText] value: %f", balance.value);
        return String.format("%.2f %s", balance.value, currencyValue);
    }

    public static void launchUrl(Context context, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    public static void showConfirm(
            Context context, int msg, DialogInterface.OnClickListener callback) {
        new AlertDialog.Builder(context)
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, callback)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    public static void showAlertText(Context context, String msg) {
        new AlertDialog.Builder(context)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void showAlertText(Context context, @StringRes int msgResId) {
        showAlertText(context, context.getString(msgResId));
    }

    public static String formatAccountFullName(Contact contact) {
        return String.format("%s %s", contact.firstName, contact.lastName);
    }

    public static ProgressDialog showProgress(Context context, @StringRes int msg) {
        final ProgressDialog dialog = new ProgressDialog(
                new ContextThemeWrapper(context, R.style.AppThemeDialog)
        );
        dialog.setMessage(context.getString(msg));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setIndeterminateDrawable(ContextCompat.getDrawable(context, R.drawable.progress));
        dialog.show();
        return dialog;
    }

    @NonNull
    public static Contact p2pChatPartner(@NonNull Chat chat) {
        ArrayList<Contact> contacts = chat.getContacts();

        if (contacts.size() == 1) {
            return contacts.get(0);
        }

        if (contacts.get(0).iAm) {
            return contacts.get(1);
        }

        return contacts.get(0);
    }
//
//    public static String buildChatMessageText(Context context, ChatMessage message) {
//        String content = message.getContent();
//
//        if (ChatMessage.TYPE_TEXT_MESSAGE.equals(message.getType())) {
//            return content;
//        } else if (ChatMessage.TYPE_SUBJECT.equals(message.getType())) {
//            return context.getResources().getString(R.string.chat_has_rename, content);
//        } else if (ChatMessage.TYPE_NOTIFICATION.equals(message.getType())) {
//            ChatNotificationData cnd = message.getNotificationData();
//            if (cnd != null) {
//                return Utils.buildChatNotificationMessageText(context, message.getSender(), cnd).toString();
//            }
//        }
//
//        return null;
//    }

    public static Spanned buildChatMessageText(Context context, ChatMessage message) {
        String content = null;

        if (message.isContactMessage()) {
            content = context.getString(R.string.last_message_contact, Utils.formatAccountFullName(message.getSender()));
        } else if (message.isDeletedMessage()) {
            content = context.getString(R.string.last_message_removed, Utils.formatAccountFullName(message.getSender()));
        } else if (message.isQuoteMessage()) {
            content = context.getString(R.string.last_message_quoted, Utils.formatAccountFullName(message.getSender()));
        }

        if (!TextUtils.isEmpty(content)) {
            return Html.fromHtml(content);
        }
        return new SpannedString("");
    }

    public static Spanned buildChatNotificationMessageText(Context context, Contact sender, ChatNotificationData cnd) {
        StringBuilder sb = new StringBuilder();

        if (TextUtils.equals(cnd.getType(), ChatNotificationData.CHAT_NOTIFICATION_TYPE_CREATE)) {
            sb.append(context.getString(R.string.user))
                    .append(" <b>")
                    .append(Utils.formatAccountFullName(sender))
                    .append("</b> ")
                    .append(context.getString(R.string.notification_message_create));
        } else if (TextUtils.equals(cnd.getType(), ChatNotificationData.CHAT_NOTIFICATION_TYPE_INVITE)) {
            sb.append("<b>")
                    .append(Utils.formatAccountFullName(sender))
                    .append("</b> ")
                    .append(context.getString(R.string.notification_message_invite))
                    .append(" <b>")
                    .append(joinContacts(cnd.getContacts(), ','))
                    .append("</b>.");
        } else if (TextUtils.equals(cnd.getType(), ChatNotificationData.CHAT_NOTIFICATION_TYPE_REVOKE)) {
            sb.append("<b>")
                    .append(Utils.formatAccountFullName(sender))
                    .append("</b> ")
                    .append(context.getString(R.string.notification_message_revoke))
                    .append(" <b>")
                    .append(joinContacts(cnd.getContacts(), ','))
                    .append("</b>.");
        } else if (TextUtils.equals(cnd.getType(), ChatNotificationData.CHAT_NOTIFICATION_TYPE_LEAVE)) {
            sb.append("<b>")
                    .append(Utils.formatAccountFullName(sender))
                    .append("</b> ")
                    .append(context.getString(R.string.notification_message_leave));
        } else if (TextUtils.equals(cnd.getType(), ChatNotificationData.CHAT_NOTIFICATION_TYPE_REMOVE)) {
            sb.append("<b>")
                    .append(Utils.formatAccountFullName(sender))
                    .append("</b> ")
                    .append(context.getString(R.string.notification_message_remove));
        }

        return Html.fromHtml(sb.toString());
    }

    public static String joinContacts(List<Contact> contacts, char separator) {
        StringBuilder sb = new StringBuilder();

        if (CollectionUtils.isNotEmpty(contacts)) {
            for (int i = 0, size = contacts.size(); i < size; ++i) {
                Contact c = contacts.get(i);
                sb.append(Utils.formatAccountFullName(c));

                if (i < size - 1) {
                    sb.append(separator).append(' ');
                }
            }
        }

        return sb.toString();
    }

    public static void setVisibility(@NonNull View view, int visibility) {
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    public static void setVisibilityGone(@NonNull View view) {
        setVisibility(view, View.GONE);
    }

    public static void setVisibilityVisible(@NonNull View view) {
        setVisibility(view, View.VISIBLE);
    }

    @Nullable
    public static PowerManager.WakeLock setupProximityWakeLock(@NonNull Context context) {
        PowerManager.WakeLock proximityWakeLock = null;

        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
            }
        } else {
            proximityWakeLock = pm.newWakeLock(32, TAG);
        }

        if (proximityWakeLock != null) {
            proximityWakeLock.acquire();
        } else {
            D.log("ProximitySensor", "Proximity sensor unsupported");
        }

        return proximityWakeLock;
    }


    public static void releaseProximityWakeLock(@Nullable PowerManager.WakeLock proximityWakeLock) {
        if (proximityWakeLock == null) {
            return;
        }

        if (proximityWakeLock.isHeld()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                proximityWakeLock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY);
            } else {
                proximityWakeLock.release();
            }
        }
    }

    public static void viewGroupEnabled(ViewGroup viewGroup, boolean enabled) {
        viewGroup.setEnabled(enabled);

        for (int i = 0, count = viewGroup.getChildCount(); i < count; ++i) {
            View v = viewGroup.getChildAt(i);

            if (v instanceof ViewGroup) {
                viewGroupEnabled((ViewGroup) v, enabled);
            } else {
                v.setEnabled(enabled);
            }
        }
    }

    @NonNull
    public static String extractSip(@NonNull String sipNumber) {
        if (TextUtils.isEmpty(sipNumber)) {
            return "";
        }

        String realNumber = TextUtils.split(sipNumber, "@")[0];
        final boolean favorite = sipNumber.startsWith(DataProvider.FAVORITE_MARKER);
        if (favorite) {
            realNumber = realNumber.replace(DataProvider.FAVORITE_MARKER, "");
        }

        return realNumber;
    }

    @Nullable
    public static String getCallIdentity(Call call) {
        if (call.contact == null) {
            final String identity = call.identity.replace("%23", "#");
            final int npos = identity.indexOf('@');
            if (npos > 0) {
                return identity.substring(0, npos);
            } else {
                return identity;
            }
        } else {
            return String.format("%s %s", call.contact.firstName, call.contact.lastName);
        }
    }


    public static Drawable getDrawable(final Context context, @DrawableRes int drawableId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(drawableId);
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(drawableId);
        }
    }

    public static String formatTime(Date date) {
        return sTimeFormat.format(date);
    }

    public static String formatTime(long millis) {
        return formatTime(new Date(millis));
    }

    public static String formatDateTime(Context context, DateTime dateTime) {
        return formatDateTime(context, dateTime.getMillis());
    }

    public static String formatDateTime(Context context, long millis) {
        final Date date = new Date(millis);

        if (DateTimeUtils.isToday(millis)) {
            return context.getString(R.string.chat_date_today) + " " + formatTime(date);
        } else if (DateTimeUtils.isYesterday(millis)) {
            return context.getString(R.string.chat_date_yesterday) + " " + formatTime(date);
        } else {
            final SimpleDateFormat timeDateSdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return timeDateSdf.format(date);
        }
    }

    public static String formatDateTimeShort(long millis) {
        final Date date = new Date(millis);

        if (DateTimeUtils.isToday(millis)) {
            return formatTime(date);
        } else if (DateTimeUtils.isThisYear(millis)) {
            return sThisYearFormat.format(date);
        } else {
            return sPreviousYearFormat.format(date);
        }

    }

    public static void setupActionButtonsForContact(@NonNull ImageButton left, @NonNull ImageButton right, @NonNull Contact contact, @NonNull BaseFragment.FragmentActionListener fragmentActionListener, Function<Contact, Void> sendRequest) {
        final Context context = left.getContext();

        if (contact.isDodicall()) {
            if (contact.invite ||
                    contact.subscriptionRequest ||
                    contact.isBlocked() ||
                    contact.isDeclinedRequest) {
                Utils.setVisibilityGone(left);
                Utils.setVisibilityVisible(right);

                setAction(right, R.drawable.phone_offline, v -> fragmentActionListener.onContactCall(contact));
            } else if (contact.id == 0) {
                Utils.setVisibilityVisible(left);
                Utils.setVisibilityVisible(right);
                setAction(left, R.drawable.phone_offline, v -> fragmentActionListener.onContactCall(contact));
                setAction(right, R.drawable.accept_user_request, v -> sendRequest.apply(contact));
            } else {
                Utils.setVisibilityVisible(left);
                Utils.setVisibilityVisible(right);

                setAction(left, R.drawable.contacts_list_item_call_ic, v -> fragmentActionListener.onContactCall(contact));
                setAction(right, R.drawable.contacts_list_item_chat_ic, v -> CreateChatAsyncTask.execute(context, contact, chat -> {
                    if (context != null)
                        context.startActivity(ChatActivity.newIntent(context, chat));
                }));

                int imageLevel = StatusesAdapter.getStatusDrawableLevel(contact.getStatus());
                left.setImageLevel(imageLevel);
                right.setImageLevel(imageLevel);
            }
        } else {
            if (contact.isSaved()) {
                Utils.setVisibilityGone(left);
                Utils.setVisibilityVisible(right);
                setAction(right, R.drawable.phone_pstn, v -> fragmentActionListener.onContactCall(contact));
            } else {
                Utils.setVisibilityVisible(left);
                setAction(left, R.drawable.phone_pstn, v -> fragmentActionListener.onContactCall(contact));

                Utils.setVisibilityVisible(right);
                setAction(right, R.drawable.ic_add_to_saved, v -> {
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.contact_save_confirm_msg)
                                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                        if (DialogInterface.BUTTON_POSITIVE == which) {
                                            new ContactQueryHandler(context, contact, null, false).startInsert(
                                                    ContactQueryHandler.ACTION_SAVE_PHONEBOOK_CONTACT, null,
                                                    ContentUris.withAppendedId(
                                                            DataProvider.CONTACTS_URI,
                                                            DataProvider.PHONEBOOK_MAGIC_ID +
                                                                    Integer.valueOf(contact.phonebookId)
                                                    ),

                                                    contact.toContentValues()
                                            );
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, null)
                                    .show();
                        }
                );
            }
        }
    }

    public interface IdentityCallback {
        void onCreateNewContact(String identity);

        void onAddToExistContact(String identity);

        void onStartCall(String identity);
    }

    public static void setupActionButtonsForIdentity(@NonNull ImageButton left, @NonNull ImageButton right, @NonNull String identity, @NonNull IdentityCallback identityCallback) {
        final Context context = left.getContext();

        String[] items = new String[]{
                context.getString(R.string.new_contact),
                context.getString(R.string.add_to_exist_contact)
        };

        Utils.setVisibilityVisible(left);
        setAction(left, R.drawable.phone_pstn, v -> identityCallback.onStartCall(identity));

        Utils.setVisibilityVisible(right);
        setAction(right, R.drawable.ic_add_to_saved, v -> {
            new AlertDialog.Builder(context)
                    .setItems(items, (dialog, which) -> {
                        switch (which) {
                            case 0: {
                                identityCallback.onCreateNewContact(identity);
                                break;
                            }
                            case 1: {
                                identityCallback.onAddToExistContact(identity);
                                break;
                            }
                        }
                    })
                    .show();

        });
    }

    static void setAction(@NonNull ImageButton button, @DrawableRes int actionImage, @Nullable View.OnClickListener actionListener) {
        button.setImageResource(actionImage);
        button.setOnClickListener(actionListener);
    }

    public static void copyFile(String from, String to) {
        copyFile(new File(from), new File(to));
    }

    public static void copyFile(File from, File to) {
        try {
            InputStream in = new FileInputStream(from);
            OutputStream out = new FileOutputStream(to);
            copyFile(in, out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatVersion(Context context, int serverArea, ServerAreaModelWrapper areaModel) {
        String appVersion = AppDeviceInfo.getVersionName(context);
        String blVersion = BL.getVersion();
        String serverAreaName = getAreaName(areaModel, Utils.getLocale(context));

        if (TextUtils.isEmpty(appVersion)) {
            appVersion = "Unknown";
        }

        if (TextUtils.isEmpty(blVersion)) {
            blVersion = "Unknown";
        }

        if (serverArea == 0) {
            int npos = appVersion.lastIndexOf('.');
            if (npos > 0) {
                appVersion = appVersion.substring(0, npos);
            }

            npos = blVersion.lastIndexOf('.');
            if (npos > 0) {
                blVersion = blVersion.substring(0, npos);
            }

            serverAreaName = "";
        }

        return context.getString(R.string.app_version_number, appVersion, blVersion, serverAreaName);
    }

    public static String getAreaName(ServerAreaModelWrapper serverAreaModel, String locale) {
        if (serverAreaModel == null) {
            return "";
        }

        if (TextUtils.isEmpty(locale)) {
            return serverAreaModel.getNameEn();
        }

        if (locale.startsWith("ru")) {
            return serverAreaModel.getNameRu();
        } else if (locale.startsWith("en")) {
            return serverAreaModel.getNameEn();
        } else {
            return serverAreaModel.getNameEn();
        }
    }

    @Nullable
    public static ServerAreaModelWrapper getAreaByKey(ServerAreasList list, int key) {
        ServerAreaModelWrapper sam = null;

        for (int i = 0; i < list.size(); ++i) {
            ServerAreaModelWrapper area = list.get(i);
            if (area.getKey() == key) {
                sam = area;
                break;
            }
        }

        return sam;
    }

    public static int getLastServerArea(Context context) {
        return Preferences.get(context).getInt(Preferences.Fields.PREF_LAST_SERVER_AREA, 0);
    }

    public static String getLastServerAreaUrl(Context context) {
        return Preferences.get(context).getString(
                Preferences.Fields.PREF_LAST_SERVER_AREA_URL,
                context.getString(R.string.balance_url_industrial)
        );
    }
}
