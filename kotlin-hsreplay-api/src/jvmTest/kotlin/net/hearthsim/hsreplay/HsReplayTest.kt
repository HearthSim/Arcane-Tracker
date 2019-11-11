package net.hearthsim.hsreplay

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.put
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.InternalAPI
import net.hearthsim.hsreplay.model.legacy.HSPlayer
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import kotlinx.coroutines.*
import net.hearthsim.analytics.DefaultAnalytics
import net.hearthsim.console.DefaultConsole
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

    @Test
    fun testUpload() {
        val hsReplay = HsReplay(userAgent = "net.mbonnin.arcanetracker/4.13; Android 9;",
                console = DefaultConsole(),
                preferences = preferences,
                analytics = DefaultAnalytics())

        val uploadRequest = UploadRequest(
                match_start = "2019-07-13T13:05:44+0200",
                spectator_mode = false,
                game_type = 2, // ranked_standard
                format = 2, // standard
                build = 31353,
                friendly_player = "2",
                player1 = HSPlayer(
                        rank = 5
                ),
                player2 = HSPlayer(
                        rank = 5
                )
        )

        val dir = System.getProperty("user.dir")
        val text = File(dir, "src/jvmTest/files/power.log").readText()
        runBlocking {
            val result = hsReplay.uploadGame(uploadRequest, text)
            if (result is HsReplay.UploadResult.Failure) {
                result.e.printStackTrace()
            }
        }
    }

    @Test
    fun testBattleGroundsUpload() {
        val hsReplay = HsReplay(userAgent = "net.mbonnin.arcanetracker/4.13; Android 9;",
                console = DefaultConsole(),
                preferences = preferences,
                analytics = DefaultAnalytics())

        val uploadRequest = UploadRequest(
                match_start = "2019-07-13T13:05:44+0200",
                build = 31353,
                spectator_mode = false,
                format = 1, // Wild
                game_type = 50 // Battlegrounds
        )

        val text = File("/home/martin/dev/hsdata/2019_11_11_battlegrounds").readText()
        runBlocking {
            val result = hsReplay.uploadGame(uploadRequest, text)
            if (result is HsReplay.UploadResult.Failure) {
                result.e.printStackTrace()
            }
        }
    }
    @InternalAPI
    @Test
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
}