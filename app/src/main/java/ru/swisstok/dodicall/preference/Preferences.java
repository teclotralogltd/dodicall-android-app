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

package ru.swisstok.dodicall.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

public class Preferences {

    private static Preferences preferences;
    private final SharedPreferences sharedPreferences;
//    public static final String PREFERENCES_FILE = "ru.swisstok.dodicall.preferences";

    public static class UnsupportedPreferenceKeyException extends RuntimeException {

        public UnsupportedPreferenceKeyException() {
            super();
        }

        public UnsupportedPreferenceKeyException(String message) {
            super(message);
        }

        public UnsupportedPreferenceKeyException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedPreferenceKeyException(Throwable cause) {
            super(cause);
        }
    }

    public static final class HEADERS {
        public static final String COMMON = "pref_header_common";
        public static final String TELEPHONY = "pref_header_telephony";
        public static final String CHATS = "pref_header_chats";
        public static final String INTERFACE = "pref_header_interface";
        public static final String INFO = "pref_header_info";
        public static final String DEBUG = "pref_header_debug";
    }

    //TODO: use resources string; singleton;
    public static final class Fields {
        public static final String PREF_DIALPAD_LAST_VAL = "pref_dialpdad_last_val";
        public static final String PREF_DIALPAD_OPEN = "pref_dialpdad_open";
        public static final String PREF_LAST_SERVER_AREA = "pref_last_server_area";
        public static final String PREF_LAST_SERVER_AREA_URL = "pref_last_server_area_url";
        public static final String PREF_CHATS_FONT_SIZE = "pref_chats_font_size";
        public static final String PREF_CHAT_IMPORT_KEY = "pref_chats_import_key";
        public static final String PREF_CHAT_EXPORT_KEY = "pref_chats_export_key";
        public static final String PREF_CHATS_CLEAR = "pref_chats_clear";
        public static final String PREF_COMMON_WHITE_LIST = "pref_common_white_list";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_OPUS = "pref_debug_codecs_audio_wifi_opus";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_SPEEX32KHZ = "pref_debug_codecs_audio_wifi_speex32khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_SPEEX16KHZ = "pref_debug_codecs_audio_wifi_speex16khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_SPEEX8KHZ = "pref_debug_codecs_audio_wifi_speex8khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_PCMU = "pref_debug_codecs_audio_wifi_pcmu";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_PCMA = "pref_debug_codecs_audio_wifi_pcma";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_G722 = "pref_debug_codecs_audio_wifi_g722";
        public static final String PREF_DEBUG_CODECS_AUDIO_WIFI_G729 = "pref_debug_codecs_audio_wifi_g729";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_OPUS = "pref_debug_codecs_audio_cell_opus";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_SPEEX32KHZ = "pref_debug_codecs_audio_cell_speex32khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_SPEEX16KHZ = "pref_debug_codecs_audio_cell_speex16khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_SPEEX8KHZ = "pref_debug_codecs_audio_cell_speex8khz";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_PCMU = "pref_debug_codecs_audio_cell_pcmu";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_PCMA = "pref_debug_codecs_audio_cell_pcma";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_G722 = "pref_debug_codecs_audio_cell_g722";
        public static final String PREF_DEBUG_CODECS_AUDIO_CELL_G729 = "pref_debug_codecs_audio_cell_g729";
        public static final String PREF_DEBUG_CODECS_VIDEO_VP8 = "pref_debug_codecs_video_vp8";
        public static final String PREF_DEBUG_CODECS_VIDEO_MP4VES = "pref_debug_codecs_video_mp4ves";
        public static final String PREF_DEBUG_CODECS_VIDEO_H264 = "pref_debug_codecs_video_h264";
        public static final String PREF_DEBUG_SEND_REPORT_SUBJECT = "pref_debug_send_report_subject";
        public static final String PREF_DEBUG_SEND_REPORT_MESSAGE = "pref_debug_send_report_message";
        public static final String PREF_DEBUG_SEND_REPORT_CALLS_LOG = "pref_debug_send_report_calls_log";
        public static final String PREF_DEBUG_SEND_REPORT_CALLS_HISTORY_LOG = "pref_debug_send_report_calls_history_log";
        public static final String PREF_DEBUG_SEND_REPORT_CALLS_QUALITY_LOG = "pref_debug_send_report_calls_quality_log";
        public static final String PREF_DEBUG_SEND_REPORT_CHATS_LOG = "pref_debug_send_report_chats_log";
        public static final String PREF_DEBUG_SEND_REPORT_DB_LOG = "pref_debug_send_report_db_log";
        public static final String PREF_DEBUG_SEND_REPORT_QUERIES_LOG = "pref_debug_send_report_queries_log";
        public static final String PREF_DEBUG_SEND_REPORT_TRACE_LOG = "pref_debug_send_report_trace_log";
        public static final String PREF_DEBUG_MODE = "pref_debug_mode";
        public static final String PREF_DEBUG_CODECS = "pref_debug_codecs";
        public static final String PREF_DEBUG_CALLS_LOG = "pref_debug_calls_log";
        public static final String PREF_DEBUG_CALLS_HISTORY = "pref_debug_calls_history";
        public static final String PREF_DEBUG_CALLS_QUALITY_LOG = "pref_debug_calls_quality_log";
        public static final String PREF_DEBUG_CHAT_LOG = "pref_debug_chat_log";
        public static final String PREF_DEBUG_DB_LOG = "pref_debug_db_log";
        public static final String PREF_DEBUG_TRACE_LOG = "pref_debug_trace_log";
        public static final String PREF_DEBUG_APPLICATION_LOG = "pref_debug_application_log";
        public static final String PREF_DEBUG_QUERIES_LOG = "pref_debug_queries_log";
        public static final String PREF_DEBUG_SEND_BUG = "pref_debug_send_bug";
        public static final String PREF_INFO_ABOUT = "pref_info_about";
        public static final String PREF_INFO_WHAT_NEW = "pref_info_what_new";
        public static final String PREF_INFO_KNOWN_ISSUES = "pref_info_known_issues";
        public static final String PREF_INFO_HELP = "pref_info_help";
        public static final String PREF_INTERFACE_STYLE = "pref_interface_style";
        public static final String PREF_INTERFACE_LANGUAGE = "pref_interface_language";
        public static final String PREF_INTERFACE_ANIMATION = "pref_interface_animation";
        public static final String PREF_TELEPHONY_ENCRYPTION = "pref_telephony_encryption";
        public static final String PREF_TELEPHONY_ACCOUNT_DEFAULT = "pref_telephony_account_default";
        public static final String PREF_TELEPHONY_VIDEO_ENABLED = "pref_telephony_video_enabled";
        public static final String PREF_TELEPHONY_VIDEO_RESOLUTION_WIFI = "pref_telephony_video_resolution_wifi";
        public static final String PREF_TELEPHONY_VIDEO_RESOLUTION_CELL = "pref_telephony_video_resolution_cell";
        public static final String PREF_TELEPHONY_NOISE_SUPPRESSION = "pref_telephony_noise_suppression";
    }

    public static final class TableColumn {
        public static final String CHATS_FONT_SIZE = "GuiFontSize";
        public static final String COMMON_WHITE_LIST = "DoNotDesturbMode";
        public static final String DEBUG_MODE = "TraceMode";
        public static final String INTERFACE_LANGUAGE = "GuiLanguage";
        public static final String INTERFACE_ANIMATION = "GuiAnimation";
    }

    private Preferences(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static synchronized Preferences getInstance(Context context) {
        if (preferences == null) {
            preferences = new Preferences(context);
        }
        return preferences;
    }

    public static SharedPreferences get(Context context) {
        return getInstance(context).sharedPreferences;
    }

}
