package net.mbonnin.arcanetracker

import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * Created by martin on 10/14/16.
 */

object Settings {
    val SHOW_NEXT_TIME = "SHOW_NEXT_TIME"
    val ALPHA = "alpha"
    val CHECK_IF_RUNNING = "CHECK_IF_RUNNING"
    val AUTO_SELECT_DECK = "AUTO_SELECT_DECK"
    val AUTO_ADD_CARDS = "AUTO_ADD_CARDS"
    val AUTO_QUIT = "AUTO_QUIT"
    val DRAWER_WIDTH = "DRAWER_WIDTH"
    val BUTTON_WIDTH = "BUTTON_WIDTH"
    val LOCALE = "LOCALE"
    val DEFAULT_HSREPLAY = false
    val CARDSDB_VERSION = "CARDSDB_VERSION"
    val LANGUAGE = "LANGUAGE"
    val SHOW_XIAOMI_WARNING = "SHOW_XIAOMI_WARNING"
    val PICASSO_CARD_REQUEST_HANDLER_VERSION = "PICASSO_CARD_REQUEST_HANDLER_VERSION"
    val VERSION = "VERSION"
    val SHOW_CHANGELOG = "SHOW_CHANGELOG"
    val HSREPLAY_USERNAME = "HSREPLAY_USERNAME"
    val SCREEN_CAPTURE_ENABLED = "SCREEN_CAPTURE_ENABLED"
    val SCREEN_CAPTURE_RATIONALE_SHOWN = "SCREEN_CAPTURE_RATIONALE_SHOWN"
    val QUIT_TIMEOUT = "QUIT_TIMEOUT"
    val ONBOARDING_FINISHED = "ONBOARDING_FINISHED"
    val HSREPLAY_OAUTH_REFRESH_TOKEN = "HSREPLAY_OAUTH_REFRESH_TOKEN"
    val HSREPLAY_OAUTH_ACCESS_TOKEN = "HSREPLAY_OAUTH_ACCESS_TOKEN"

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(ArcaneTrackerApplication.context)

    operator fun get(key: String, defaultValue: Boolean): Boolean {
        return preferences.getBoolean(key, defaultValue)
    }

    operator fun get(key: String, defaultValue: Int): Int {
        return preferences.getInt(key, defaultValue)
    }

    fun getString(key: String, defaultValue: String?): String? {
        return preferences.getString(key, defaultValue)
    }

    operator fun set(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    operator fun set(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    operator fun set(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}
