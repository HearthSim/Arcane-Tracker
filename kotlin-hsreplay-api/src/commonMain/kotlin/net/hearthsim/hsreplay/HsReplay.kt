package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.DEFAULT
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.io.readRemaining
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.hearthsim.analytics.Analytics
import net.hearthsim.console.Console
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_BATTLETAG
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_LEGACY_TOKEN
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_PREMIUM
import net.hearthsim.hsreplay.Preferences.Companion.KEY_HSREPLAY_USERNAME
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.legacy.UploadToken
import net.hearthsim.hsreplay.model.new.Account
import net.hearthsim.hsreplay.model.new.ClaimInput
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import net.hearthsim.hsreplay.model.new.CollectionUploadRequest

class HsReplay(val preferences: Preferences, val console: Console, val analytics: Analytics, val userAgent: String) {
    private val oauthApi = HsReplayOauthApi(userAgent)
    private val legacyApi = HsReplayLegacyApi(userAgent)
    private val accessTokenProvider = AccessTokenProvider(preferences, oauthApi, analytics)
    private val newApi = HsReplayNewApi(userAgent, accessTokenProvider)
    private val s3Api = HsReplayS3Api(userAgent)

    private var account: Account? = null
    private var uploadTokenKey: String? = null

    init {
        uploadTokenKey = preferences.getString(KEY_HSREPLAY_LEGACY_TOKEN)

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

    fun hasValidAccessToken(): Boolean {
        return accessTokenProvider.isLoggedIn()
    }

    fun account(): Account? {
        /*
         * It might happen that we don't have a valid access token. One possible reason is that the backend had refresh_token rotation activated before mid-may 2019
         * This was removed in https://github.com/HearthSim/HSReplay.net/commit/e1ccc5bf35c73cbc60fa525f737a1aa238cab977#diff-b21dfdea9195d8cf6d85ec5b850218b8
         *
         * But we still have a few users in the wild with an invalid accessToken. If everything goes well, this number should decrease as users reinstall the app
         * and new users come.
         *
         * As of 2019-07-13, it's 2.6k users every 28 days out of 29k.
         */
//        if (!hasValidAccessToken()) {
//            return null
//        }
        if (uploadTokenKey == null) {
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
            console.error(Exception(e))
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

    sealed class LoginResult {
        object Success : LoginResult()
        class Failure(val e: Throwable) : LoginResult()
    }

    fun setTokens(accessToken: String, refreshToken: String) {
        accessTokenProvider.remember(accessToken, refreshToken)
    }

    suspend fun login(code: String) = coroutineScope {
        val uploadTokenDeferred = async {
            try {
                legacyApi.createUploadToken()
            } catch (e: Exception) {
                return@async e
            }
        }

        val token = try {
            oauthApi.login(code)
        } catch (e: Exception) {
            return@coroutineScope LoginResult.Failure(e)
        }

        val uploadToken = uploadTokenDeferred.await()
        if (uploadToken !is UploadToken) {
            return@coroutineScope LoginResult.Failure(uploadToken as Exception)
        }

        accessTokenProvider.remember(token.access_token, token.refresh_token)

        try {
            newApi.claimToken(ClaimInput(uploadToken.key))
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope LoginResult.Failure(e)
        }

        account = try {
            newApi.account()
        } catch (e: Exception) {
            accessTokenProvider.forget()
            return@coroutineScope LoginResult.Failure(e)
        }

        uploadTokenKey = uploadToken.key
        preferences.putString(KEY_HSREPLAY_LEGACY_TOKEN, uploadToken.key)

        preferences.putBoolean(KEY_HSREPLAY_PREMIUM, account!!.is_premium)
        preferences.putString(KEY_HSREPLAY_BATTLETAG, account!!.battletag)
        preferences.putString(KEY_HSREPLAY_USERNAME, account!!.username)

        return@coroutineScope LoginResult.Success
    }

    sealed class UploadResult {
        class Success(val replayUrl: String) : UploadResult()
        class Failure(val e: Throwable) : UploadResult()
    }

    suspend fun uploadGame(uploadRequest: UploadRequest, gameStr: ByteArray): UploadResult {
        console.debug("uploadGame [token=$uploadTokenKey]")

        if (uploadTokenKey == null) {
            return UploadResult.Failure(Exception("no token"))
        }

        val authorization = "Token $uploadTokenKey"

        val upload = try {
            legacyApi.createUpload(uploadRequest, authorization)
        } catch (e: Exception) {
            return UploadResult.Failure(e)
        }

        console.debug("url is ${upload.url}")
        console.debug("put_url is ${upload.put_url}")

        val response = try {
            s3Api.put(putUrl = upload.put_url, gameString = gameStr)
        } catch (e: Exception) {
            console.error(Exception(e))
            return UploadResult.Failure(e)
        }

        if (response.status.value / 100 != 2) {
            return UploadResult.Failure(Exception("Bad status: ${response.status.value}: ${response.content.readRemaining().readText()}"))
        }

        return UploadResult.Success(upload.url)
    }

    sealed class CollectionUploadResult {
        object Success : CollectionUploadResult()
        class Failure(val code: Int, val throwable: Throwable) : CollectionUploadResult()
    }

    suspend fun uploadCollection(collectionUploadData: CollectionUploadData, account_hi: String, account_lo: String): CollectionUploadResult {
        try {
            // Get the account first to ensure a valid access token else the collection upload route returns 400 instead of 401
            newApi.account()
        } catch (throwable: Throwable) {
            return CollectionUploadResult.Failure(101, throwable)
        }

        val uploadCollectionRequest = try {
            newApi.collectionUploadRequest(account_hi, account_lo)
        } catch (throwable: Throwable) {
            return CollectionUploadResult.Failure(102, throwable)
        }

        val s3Client = HttpClient {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        console.debug(message)
                    }
                }
                level = LogLevel.NONE
            }
            install(JsonFeature) {
                serializer = KotlinxSerializer(Json.nonstrict).apply {
                    setMapper(CollectionUploadData::class, CollectionUploadData.serializer())
                }
            }
        }

        try {
            val response = s3Client.put<HttpResponse>(uploadCollectionRequest.url) {
                header("User-Agent", userAgent)
                contentType(ContentType.Application.Json)

                body = collectionUploadData
            }

            if (response.status.value / 100 != 2) {
                return CollectionUploadResult.Failure(response.status.value * 1000 + 103, Exception("HTTP error during collection upload"))
            }
            return CollectionUploadResult.Success
        } catch (throwable: Throwable) {
            return CollectionUploadResult.Failure(102, throwable)
        }
    }
}