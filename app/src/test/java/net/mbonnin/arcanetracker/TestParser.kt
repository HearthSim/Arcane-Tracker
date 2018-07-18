package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.arcanetracker.parser.PowerParser
import net.mbonnin.hsmodel.CardJson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class TestParser {

    internal open class SimpleListener : GameLogic.Listener {
        var game: Game? = null

        override fun gameStarted(game: Game) {
            this.game = game
        }

        override fun gameOver() {

        }

        override fun somethingChanged() {

        }
    }

    private fun runParser(logFileName: String, listener: GameLogic.Listener) {
        GameLogic.get().addListener(listener)
        val powerParser = PowerParser({ tag -> GameLogic.get().handleRootTag(tag) }, {_, _ -> Unit})

        val client = OkHttpClient()
        val request = Request.Builder().url("https://raw.githubusercontent.com/HearthSim/hsreplay-test-data/master/data/${logFileName}").get().build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception()
        }

        val inputStream = response.body()!!.byteStream()

        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        powerParser.onPreviousDataRead()
        while (true) {
            line = br.readLine()
            if (line == null) {
                break
            }
            powerParser.onLine(line)
        }
        GameLogic.get().removeListener(listener)
    }

    @Test
    fun testSpectator() {
        class InterruptedListener : SimpleListener() {
            var gameOverCount: Int = 0

            override fun gameOver() {
                gameOverCount++
            }
        }

        val listener = InterruptedListener()
        runParser("spectator.log", listener)

        Assert.assertTrue(listener.gameOverCount == 1)
    }

    companion object {

        @BeforeClass
        fun beforeClass() {
            Timber.plant(TestTree())
            CardJson.init("enUS", ArrayList())
        }
    }

}