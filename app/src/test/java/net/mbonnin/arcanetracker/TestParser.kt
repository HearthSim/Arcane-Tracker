package net.mbonnin.arcanetracker

import kotlinx.io.streams.asInput
import net.hearthsim.kotlin.hslog.PowerParser
import net.mbonnin.arcanetracker.hslog.Game
import net.mbonnin.arcanetracker.hslog.GameLogic
import net.mbonnin.hsmodel.CardJson
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader

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

    private fun runParser(inputStream: InputStream, listener: GameLogic.Listener) {
        GameLogic.get().addListener(listener)
        val powerParser = PowerParser({ tag -> GameLogic.get().handleRootTag(tag) }, null, null)
        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        System.out.println("Running...")
        while (true) {
            line = br.readLine()
            if (line == null) {
                break
            }
            powerParser.process(line, processGameTags = true)
        }
        GameLogic.get().removeListener(listener)
    }

    private fun runParser(logFileName: String, listener: GameLogic.Listener) {
        val client = OkHttpClient()
        val request = Request.Builder().url("https://raw.githubusercontent.com/HearthSim/hsreplay-test-data/master/data/${logFileName}").get().build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception()
        }

        val inputStream = response.body()!!.byteStream()
        runParser(inputStream, listener)
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

    @Test
    fun testZayle() {
        class InterruptedListener : SimpleListener() {
            override fun gameStarted(game: Game) {
                super.gameStarted(game)

                System.out.println("GameLogicListener.isPlayerZayle(game)=${GameLogic.isPlayerZayle(game)}")
            }
            override fun gameOver() {
            }
        }

        val listener = InterruptedListener()
        runParser("zayle.log", listener)
    }

    companion object {

        @BeforeClass
        fun beforeClass() {
            Timber.plant(TestTree())
            val input = File("/home/martin/git/Arcane-Tracker/app/src/main/res/raw/cards.json").inputStream().asInput()
            CardJson.init("enUS", null, input)
        }
    }

}