package net.hearthsim.hsreplay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_BATTLETAG
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_LEGACY_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_PREMIUM
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_USERNAME
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput

class HsReplay(val preferences: Preferences, val console: Console, val userAgent: String) {
    private val oauthApi = HsReplayOauthApi()
    private val legacyApi = HsReplayLegacyApi()
    private val accessTokenProvider = AccessTokenProvider(preferences, oauthApi)
    private val newApi = HsReplayNewApi(accessTokenProvider)
    private val s3Api = HsReplayS3Api()

    private var account: Account? = null
    private var uploadToken: String? = null

    init {
        uploadToken = preferences.getString(KEY_HSREPLAY_LEGACY_TOKEN)

        val battletag = preferences.getString(KEY_HSREPLAY_BATTLETAG)
        val username = preferences.getString(KEY_HSREPLAY_USERNAME)
        val isPremium = preferences.getBoolean(KEY_HSREPLAY_PREMIUM)

        if (battletag != null && username != null && isPremium != null) {
            account = Account(battletag = battletag,
                    username = username,
                    is_premium = isPremium,
                    id = "")
        }
    }

    fun account(): Account? {
        if (!accessTokenProvider.isLoggedIn()) {
            return null
        }
        if (uploadToken == null) {
            return null
        }
        return account
    }

    suspend fun refreshAccountInformation() {
        try {
            account = newApi.account()
            preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
            preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
            preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)
        } catch (e: Exception) {
        }

    }
    fun logout() {
        accessTokenProvider.forget()
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, null)

        preferences.putString(KEY_HSREPLAY_BATTLETAG, null)
        preferences.putString(KEY_HSREPLAY_USERNAME, null)
        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, null)

        account = null
    }

    suspend fun login(code: String): Result<Unit> = coroutineScope {
        val uploadTokenDeferred = async {
            try {
                legacyApi.createUploadToken()
            } catch (e: Exception) {
                return@async Result.failure<Unit>(e)
            }
        }

        val token = try {
            oauthApi.login(code)
        } catch (e: Exception) {
            return@coroutineScope Result.failure<Unit>(e)
        }

        val uploadToken = uploadTokenDeferred.await()
        if (uploadToken !is UploadToken) {
            return@coroutineScope Result.failure<Unit>(uploadToken as Exception)
        }

        accessTokenProvider.remember(token.access_token, token.refresh_token)

        try {
            newApi.claimToken(ClaimInput(uploadToken.key))
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        account = try {
            newApi.account()
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope Result.failure<Unit>(e)
        }

        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, uploadToken.key)

        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
        preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
        preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)

        return@coroutineScope Result.success(Unit)
    }

    suspend fun uploadGame(uploadRequest: UploadRequest, gameStr: String): Result<String> {
        console.debug("uploadGame [token=$uploadToken]")

        if (uploadToken == null) {
            return Result.failure(Exception("no token"))
        }

        val authorization = "Token $uploadToken"

        val upload = try {
            legacyApi.createUpload(uploadRequest, authorization)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        console.debug("url is ${upload.url}")
        console.debug("put_url is ${upload.put_url}")

        try {
            s3Api.put(putUrl = upload.url, gameString = gameStr, userAgent = userAgent)
        } catch (e: Exception) {
            console.error(Exception(e))
            return Result.failure(e)
        }

        return Result.success(upload.url)
    }
}