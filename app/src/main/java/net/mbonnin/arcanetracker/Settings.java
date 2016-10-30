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
