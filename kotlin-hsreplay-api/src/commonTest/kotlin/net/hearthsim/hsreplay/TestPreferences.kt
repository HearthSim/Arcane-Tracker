package net.hearthsim.hsreplay

import kotlinx.serialization.json.Json
import okio.internal.commonToUtf8String

class TestPreferences : Preferences {
    private val map = mutableMapOf<String, Any?>()

    override fun putString(key: String, value: String?) {
        map.put(key, value)
    }

    override fun getString(key: String): String? {
        return map.get(key)?.toString()
    }

    override fun putBoolean(key: String, value: Boolean?) {
        map.put(key, value)
    }

    override fun getBoolean(key: String): Boolean? {
        return map.get(key) as Boolean?
    }

}
