package net.hearthsim.hsreplay

interface Preferences {
    fun put(key: String, value: String)
    fun get(key: String): String?

    companion object {
        val HSREPLAY_OAUTH_REFRESH_TOKEN = "HSREPLAY_OAUTH_REFRESH_TOKEN"
        val HSREPLAY_OAUTH_ACCESS_TOKEN = "HSREPLAY_OAUTH_ACCESS_TOKEN"
    }
}