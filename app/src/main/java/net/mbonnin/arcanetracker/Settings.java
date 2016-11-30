package net.mbonnin.arcanetracker;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by martin on 10/14/16.
 */

public class Settings {
    public static final String SHOW_NEXT_TIME = "SHOW_NEXT_TIME";
    public static final String ALPHA = "alpha";
    public static final String CHECK_IF_RUNNING = "CHECK_IF_RUNNING";
    public static final String AUTO_SELECT_DECK = "AUTO_SELECT_DECK";
    public static final String AUTO_ADD_CARDS = "AUTO_ADD_CARDS";
    public static final String AUTO_QUIT = "AUTO_QUIT";
    public static final String DRAWER_WIDTH = "DRAWER_WIDTH";
    public static final String BUTTON_WIDTH = "BUTTON_WIDTH";
    public static final String LOCALE = "LOCALE";
    public static final String HSREPLAY_TOKEN = "HSREPLAY_TOKEN";
    public static final String HSREPLAY = "HSREPLAY";
    public static final boolean DEFAULT_HSREPLAY = false;
    public static final String CARDSDB_VERSION = "CARDSDB_VERSION";
    public static final String LANGUAGE = "LANGUAGE";
    public static final String SHOW_XIAOMI_WARNING = "SHOW_XIAOMI_WARNING";
    public static final String PICASSO_CARD_REQUEST_HANDLER_VERSION = "PICASSO_CARD_REQUEST_HANDLER_VERSION";
    public static final String VERSION = "VERSION";

    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(ArcaneTrackerApplication.getContext());
    }

    public static boolean get(String key, boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    public static int get(String key, int defaultValue) {
        return getPreferences().getInt(key, defaultValue);
    }

    public static String get(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    public static void set(String key, boolean value) {
        getPreferences().edit().putBoolean(key, value).apply();
    }

    public static void set(String key, int value) {
        getPreferences().edit().putInt(key, value).apply();
    }

    public static void set(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }
}
