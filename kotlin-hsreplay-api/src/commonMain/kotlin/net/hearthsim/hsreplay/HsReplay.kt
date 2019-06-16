package net.hearthsim.hsreplay

class HsReplay() {
    private val oauthApi = HsReplayOauthApi()
    private val legacyApi = HsReplayLegacyApi()
    private val newApi = HsReplayNewApi {""}
}