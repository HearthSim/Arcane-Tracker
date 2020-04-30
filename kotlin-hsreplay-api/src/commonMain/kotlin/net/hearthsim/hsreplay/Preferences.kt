package net.hearthsim.hsreplay

interface Preferences {
    /**
     * pass null to remove
     */
    fun putString(key: String, value: String?)
    fun getString(key: String): String?
    fun putBoolean(key: String, value: Boolean?)
    fun getBoolean(key: String): Boolean?

    companion object {
        const val HSREPLAY_OAUTH_REFRESH_TOKEN = "HSREPLAY_OAUTH_REFRESH_TOKEN"
        const val HSREPLAY_OAUTH_ACCESS_TOKEN = "HSREPLAY_OAUTH_ACCESS_TOKEN"

        const val KEY_HSREPLAY_LEGACY_TOKEN = "HSREPLAY_TOKEN"

        const val KEY_HSREPLAY_PREMIUM = "HSREPLAY_PREMIUM"
        const val KEY_HSREPLAY_BATTLETAG = "HSREPLAY_BATTLETAG"
        const val KEY_HSREPLAY_USERNAME = "HSREPLAY_USERNAME"
    }
}