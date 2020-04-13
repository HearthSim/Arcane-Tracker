package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.InternalAPI
import kotlinx.coroutines.runBlocking
import net.hearthsim.analytics.DefaultAnalytics
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsreplay.model.legacy.HSPlayer
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.hearthsim.hsreplay.model.new.CollectionUploadData
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import java.io.File

class HsReplayTest {
    val preferences = object : Preferences {
        override fun putString(key: String, value: String?) {

        }

        override fun getString(key: String): String? {
            return when (key) {
                "HSREPLAY_TOKEN" -> "4d9dab2e-5602-4efc-9a1f-314a2a47e375"
                else -> null
            }
        }

        override fun putBoolean(key: String, value: Boolean?) {
        }

        override fun getBoolean(key: String): Boolean? {
            return null
        }

    }

    fun testUpload() {
        val hsReplay = HsReplay(userAgent = "net.mbonnin.arcanetracker/4.13; Android 9;",
            console = DefaultConsole(),
            preferences = preferences,
            analytics = DefaultAnalytics(),
            clientId = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP",
            clientSecret = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        )

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

        val dir = System.getProperty("user.dir")
        val text = File(dir, "src/jvmTest/files/power.log").readBytes()
        runBlocking {
            val result = hsReplay.uploadGame(uploadRequest, text)
            if (result is HsReplay.UploadResult.Failure) {
                result.e.printStackTrace()
            }
        }
    }

    fun testBattleGroundsUpload() {
        val hsReplay = HsReplay(userAgent = "net.mbonnin.arcanetracker/4.13; Android 9;",
            console = DefaultConsole(),
            preferences = preferences,
            analytics = DefaultAnalytics(),
            clientId = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP",
            clientSecret = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        )

        val uploadRequest = UploadRequest(
            match_start = "2019-07-13T13:05:44+0200",
            build = 31353,
            spectator_mode = false,
            format = 1, // Wild
            game_type = 50, // Battlegrounds
            players = listOf()
        )

        val text = File("/home/martin/dev/hsdata/2019_11_11_battlegrounds").readBytes()
        runBlocking {
            val result = hsReplay.uploadGame(uploadRequest, text)
            if (result is HsReplay.UploadResult.Failure) {
                result.e.printStackTrace()
            }
        }
    }

    @InternalAPI
    fun testGzip() {
        val client = HttpClient(OkHttp) {
            // install(GzipCompressFeature)
            engine {
                addInterceptor(HttpLoggingInterceptor().apply {
                    setLevel(HttpLoggingInterceptor.Level.BODY)
                })
            }
        }

        runBlocking {
            client.put<Unit>("http://127.0.0.1/test") {
                body = TextContent("Hello Martin", contentType = ContentType.Text.Plain)
            }
            Unit
        }
    }

    class DummyPreferences : Preferences {
        override fun getBoolean(key: String): Boolean? {
            return false
        }

        override fun getString(key: String): String? {
            return null
        }

        override fun putBoolean(key: String, value: Boolean?) {
        }

        override fun putString(key: String, value: String?) {
        }

    }

    fun testCollectionUpload() {
        val hsReplay = HsReplay(console = DefaultConsole(),
            userAgent = "tests",
            analytics = DefaultAnalytics(),
            preferences = DummyPreferences(),
            clientId = "pk_live_iKPWQuznmNf2BbBCxZa1VzmP",
            clientSecret = "sk_live_20180319oDB6PgKuHSwnDVs5B5SLBmh3"
        )

        hsReplay.setTokens("", "")

        runBlocking {
            val uploadData = CollectionUploadData(
                collection = mapOf(
                    "52295" to listOf(2, 1),
                    "149" to listOf(2, 1)
                ),
                cardbacks = emptyList(),
                dust = 289,
                favoriteCardback = null,
                favoriteHeroes = emptyMap(),
                gold = 2349
            )
            val result = hsReplay.uploadCollection(
                collectionUploadData = uploadData,
                account_hi = "144115198130930503",
                account_lo = "27472745"
            )

            if (result is HsReplay.CollectionUploadResult.Failure) {
                throw (result.throwable)
            }
        }
    }
}