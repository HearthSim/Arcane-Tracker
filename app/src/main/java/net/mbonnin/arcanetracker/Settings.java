package net.mbonnin.arcanetracker;

import android.content.Context;
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
    public static final String HSREPLAY_KEY = "HSREPLAY_KEY";

    private final SharedPreferences sharedPreferences;

    public Settings(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean get(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public int get(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public String get(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void set(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public void set(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public void set(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }
}
