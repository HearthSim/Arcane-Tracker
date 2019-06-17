package net.hearthsim.hsreplay

import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_ACCESS_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.HSREPLAY_OAUTH_REFRESH_TOKEN

class AccessTokenProvider(preferences: Preferences) {
    private var accessToken = preferences.get(HSREPLAY_OAUTH_ACCESS_TOKEN)
    private var refreshToken = preferences.get(HSREPLAY_OAUTH_REFRESH_TOKEN)

    fun accessToken(): String? {
        return accessToken
    }

    fun refreshToken() {}
}