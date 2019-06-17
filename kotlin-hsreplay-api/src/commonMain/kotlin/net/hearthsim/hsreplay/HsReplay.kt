package net.hearthsim.hsreplay

class HsReplay(val preferences: Preferences) {
    private val oauthApi = HsReplayOauthApi()
    private val legacyApi = HsReplayLegacyApi()
    private val accessTokenProvider = AccessTokenProvider(preferences)
    private val newApi = HsReplayNewApi {accessTokenProvider.accessToken()}


    suspend fun login(code: String): Exception? {
        val token = try {
            oauthApi.login(code)
        } catch (e: Exception) {
            return e
        }

        storeToken
        val tokenDeferred = async {
            createLegacyToken()
        }

        val oauthResult = HsReplayInterceptor.login(code)
        if (oauthResult.isFailure) {
            return@coroutineScope oauthResult.map { Unit }
        }
        val tokenResult = tokenDeferred.await()
        if (tokenResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope tokenResult.map { Unit }
        }

        val claimResult = claimToken(tokenResult.getOrNull()!!)
        if (claimResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope claimResult.map { Unit }
        }

        val accountResult = account()
        if (accountResult.isFailure) {
            HsReplayInterceptor.logout()
            return@coroutineScope accountResult.map { Unit }
        }
        accountResult.getOrNull()!!.let {
            sharedPreferences.edit {
                legacyToken = tokenResult.getOrNull()!!

                putString(KEY_HSREPLAY_LEGACY_TOKEN, legacyToken!!)

                putBoolean(KEY_HSREPLAY_PREMIUM, it.is_premium ?: false)
                putString(KEY_HSREPLAY_BATTLETAG, it.battletag)
                putString(KEY_HSREPLAY_USERNAME, it.username)
            }
        }
        return@coroutineScope Result.success(Unit)
    }
}