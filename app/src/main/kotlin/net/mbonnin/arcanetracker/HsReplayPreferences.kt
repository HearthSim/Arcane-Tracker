package net.mbonnin.arcanetracker

import android.content.Context
import android.preference.PreferenceManager
import net.hearthsim.hsreplay.Preferences


class HsReplayPreferences(context: Context): Preferences {
    val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    override fun putString(key: String, value: String?) {
        defaultSharedPreferences.edit().putString(key, value).apply()
    }
    override fun getString(key: String): String? {
        return defaultSharedPreferences.getString(key, null)
    }
    override fun putBoolean(key: String, value: Boolean?) {
        if (value == null) {
            defaultSharedPreferences.edit().remove(key).apply()
        } else {
            defaultSharedPreferences.edit().putBoolean(key, value).apply()
        }
    }
    override fun getBoolean(key: String): Boolean? {
        if (!defaultSharedPreferences.contains(key)) {
            return null
        }
        return defaultSharedPreferences.getBoolean(key, false)
    }

}