package net.hearthsim.hsreplay

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import net.hearthsim.analytics.DefaultAnalytics
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsreplay.model.legacy.HSPlayer
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import okio.internal.commonToUtf8String
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

expect fun readResource(name: String): ByteArray
expect fun runBlockingTest(block: suspend CoroutineScope.() -> Unit)

val testOauthParams = OauthParams(
        clientId = "pk_test_bEXSxNIGHk6E3q0Pu8McJpwJ", // [Test Mode] Arcane Tracker
        clientSecret = "sk_test_20180319rRyl1qhSfbZ4Vf0kJXgMfsVi", // [Test Mode] Arcane Tracker
        redirectUri = "https://localhost:9000/"
)
const val testUserAgent = "net.mbonnin.arcanetracker/4.13; Android 9;" // The server blacklists some user agents
const val testAccountHi = "144115198130930503"
const val testAccountLo = "27472745"

class MainTest {

    private val secrets by lazy {
        val bytes = readResource("secrets.json")

        val jsonObject = Json.parseJson(bytes.commonToUtf8String()).jsonObject
        val map = jsonObject.content.mapValues { it.value.primitive.content }
        map
    }

    @Test
    fun `claiming a new upload token and uploading a game is working`() {
        val preferences = TestPreferences()

        preferences.putString(Preferences.HSREPLAY_OAUTH_ACCESS_TOKEN, secrets.get(Preferences.HSREPLAY_OAUTH_ACCESS_TOKEN))
        preferences.putString(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN, secrets.get(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN))

        val accessTokenProvider = AccessTokenProvider(
                preferences = preferences,
                userAgent = testUserAgent,
                oauthParams = testOauthParams,
                analytics = DefaultAnalytics()
        )
        val hsReplay = HsReplay(
                userAgent = testUserAgent,
                preferences = preferences,
                accessTokenProvider = accessTokenProvider,
                console = DefaultConsole()
        )

        runBlockingTest {
            val loginResult = hsReplay.login()
            assertTrue(loginResult is HsReplay.LoginResult.Success)

            val legacyToken = preferences.getString(Preferences.KEY_HSREPLAY_LEGACY_TOKEN)
            println("legacyToken=$legacyToken")
            uploadGame(hsReplay)
        }
    }

    @Test
    fun `retrieving account information from the refresh token is working`() {
        val preferences = TestPreferences()

        preferences.putString(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN, secrets.get(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN))

        val accessTokenProvider = AccessTokenProvider(
                preferences = preferences,
                userAgent = testUserAgent,
                oauthParams = testOauthParams,
                analytics = DefaultAnalytics()
        )
        val hsReplay = HsReplay(
                userAgent = testUserAgent,
                preferences = preferences,
                accessTokenProvider = accessTokenProvider,
                console = DefaultConsole()
        )

        runBlockingTest {
            val accountResult = hsReplay.refreshAccountInformation()
            if (accountResult !is HSReplayResult.Success) {
                throw (accountResult as HSReplayResult.Error).exception
            }
            println("account.battletag: ${accountResult.value.battletag}")
        }
    }

    @Test
    fun `uploading collection is working as expected`() {
        val preferences = TestPreferences()

        preferences.putString(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN, secrets.get(Preferences.HSREPLAY_OAUTH_REFRESH_TOKEN))

        val accessTokenProvider = AccessTokenProvider(
                preferences = preferences,
                userAgent = testUserAgent,
                oauthParams = testOauthParams,
                analytics = DefaultAnalytics()
        )
        val hsReplay = HsReplay(
                userAgent = testUserAgent,
                preferences = preferences,
                accessTokenProvider = accessTokenProvider,
                console = DefaultConsole()
        )

        runBlockingTest {
            val uploadData = CollectionUploadData(
                    collection = mapOf(
                            "149" to listOf(2, 1),
                            "52295" to listOf(2, 1),
                            "56652" to listOf(1,1),
                            "56649" to listOf(1, 0)
                    ),
                    cardbacks = emptyList(),
                    dust = 289,
                    favoriteCardback = 0,
                    favoriteHeroes = emptyMap(),
                    gold = 2349
            )
            val result = hsReplay.uploadCollection(
                    collectionUploadData = uploadData,
                    account_hi = testAccountHi,
                    account_lo = testAccountLo
            )

            if (result is HsReplay.CollectionUploadResult.Failure) {
                throw (result.throwable)
            }
        }
    }

    @Test
    fun `loading an existing upload token and uploading a game is working`() {
        val preferences = TestPreferences()

        // no need for the accessToken to actually upload a game
        preferences.putString(Preferences.KEY_HSREPLAY_LEGACY_TOKEN, secrets.get(Preferences.KEY_HSREPLAY_LEGACY_TOKEN))

        val accessTokenProvider = AccessTokenProvider(
                preferences = preferences,
                userAgent = testUserAgent,
                oauthParams = testOauthParams,
                analytics = DefaultAnalytics()
        )
        val hsReplay = HsReplay(
                userAgent = testUserAgent,
                preferences = preferences,
                accessTokenProvider = accessTokenProvider,
                console = DefaultConsole()
        )

        runBlockingTest {
            uploadGame(hsReplay)
        }
    }

    private suspend fun uploadGame(hsReplay: HsReplay) {
        val uploadRequest = UploadRequest(
                match_start = "2019-11-13T13:05:44+0200",
                spectator_mode = false,
                game_type = 2, // ranked_standard
                format = 2, // standard
                build = 31353,
                friendly_player = "2",
                players = listOf(
                        HSPlayer(player_id = 1),
                        HSPlayer(player_id = 2)
                )
        )

        val game = readResource("power.log")
        println("uploadingGame")
        val result = hsReplay.uploadGame(uploadRequest, game)
        if (result is HsReplay.UploadResult.Failure) {
            throw(result.e)
        }
        println("game uploaded at: ${(result as HsReplay.UploadResult.Success).replayUrl}")
    }
}