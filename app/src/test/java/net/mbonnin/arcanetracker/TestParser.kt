package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.arcanetracker.parser.PowerParser
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardId
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class TestParser {

    internal open class SimpleListener : GameLogic.Listener {
        lateinit var game: Game

        override fun gameStarted(game: Game) {
            this.game = game
        }

        override fun gameOver() {

        }

        override fun somethingChanged() {

        }
    }

    @Throws(IOException::class)
    private fun runParser(resource: String, listener: GameLogic.Listener) {
        GameLogic.get().addListener(listener)
        val powerParser = PowerParser({ tag -> GameLogic.get().handleRootTag(tag) }, null!!)
        val inputStream = javaClass.getResourceAsStream(resource)
        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String

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

    @Throws(IOException::class)
    private fun runParser(resource: String): Game {
        val listener = SimpleListener()

        runParser(resource, listener)
        return listener.game
    }

    @Test
    @Throws(Exception::class)
    fun testCreatedBy() {
        val game = runParser("/created_by.log")

        Assert.assertEquals(game.findEntitySafe("73")!!.extra.createdBy, CardId.SERVANT_OF_KALIMOS)
        Assert.assertEquals(game.findEntitySafe("78")!!.extra.createdBy, CardId.SERVANT_OF_KALIMOS)
        Assert.assertEquals(game.findEntitySafe("75")!!.extra.createdBy, CardId.FROZEN_CLONE)
        Assert.assertEquals(game.findEntitySafe("76")!!.extra.createdBy, CardId.FROZEN_CLONE)
        Assert.assertEquals(game.findEntitySafe("80")!!.extra.createdBy, CardId.MIRROR_ENTITY)
    }

    @Test
    @Throws(Exception::class)
    fun testDoubleSecret() {

        val game = runParser("/double_secret.log")

        Assert.assertFalse(game.victory)

    }


    @Test
    @Throws(Exception::class)
    fun testExploreUngoro() {
        runParser("/exploreUngoro.log", object : SimpleListener() {
            override fun somethingChanged() {
                val e = game.findEntityUnsafe("99")
                if (e != null) {
                    Assert.assertTrue(e.CardID == CardId.CHOOSE_YOUR_PATH)
                }
            }
        })
    }

    @Test
    @Throws(Exception::class)
    fun testInterrupted() {
        class InterruptedListener : SimpleListener() {
            var gameOverCount: Int = 0

            override fun gameOver() {
                gameOverCount++
            }
        }

        val listener = InterruptedListener()
        runParser("/interrupted.log", listener)

        Assert.assertTrue(listener.gameOverCount == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testEndedInAttack() {
        class InterruptedListener : SimpleListener() {
            var gameOverCount: Int = 0

            override fun gameOver() {
                gameOverCount++
            }
        }

        val listener = InterruptedListener()
        runParser("/endedInAttack.log", listener)

        Assert.assertTrue(listener.gameOverCount == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testSpectator() {
        class InterruptedListener : SimpleListener() {
            var gameOverCount: Int = 0

            override fun gameOver() {
                gameOverCount++
            }
        }

        val listener = InterruptedListener()
        runParser("/spectator.log", listener)

        Assert.assertTrue(listener.gameOverCount == 1)
    }

    @Test
    @Throws(Exception::class)
    fun testNemsy() {
        val listener = object : SimpleListener() {
            override fun gameStarted(game: Game) {
                super.gameStarted(game)
                Assert.assertTrue(game.player!!.playerClass() == PlayerClass.WARLOCK)

            }
        }

        runParser("/nemsy.log", listener)
    }

    companion object {

        @BeforeClass
        fun beforeClass() {
            Timber.plant(TestTree())
            CardJson.init("enUS", ArrayList<Card>())
        }
    }

}