package net.hearthsim.hslog

import kotlinx.io.streams.asInput
import net.hearthsim.console.Console
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId
import org.junit.Test
import java.io.File

class PowerParserTest {
    val input = File("../app/src/main/res/raw/cards.json").inputStream().asInput()
    val cardJson = CardJson(lang = "enUS", injectedCards = null, input = input)

    val console = object : Console {
        override fun debug(message: String) {
            /**
             * A small hack to resolve the card ids on the fly
             */
            val regex = Regex("[a-zA-Z0-9]{2,3}_[0-9]{3}")
            val m = message.replace(regex) {
                val cardId = it.groupValues[0]
                "$cardId (${cardJson.getCard(cardId).name})"
            }
            println(m)
        }

        override fun error(message: String) {
            debug(message)
        }

        override fun error(throwable: Throwable) {
            throwable.printStackTrace()
        }

    }
    val hsLog = HSLog(console = console, cardJson = cardJson, debounceDelay = 0)

    @Test
    fun `magnetized minions appear in the opponent deck`() {
        val dir = System.getProperty("user.dir")
        val powerLines = File(dir, "src/jvmTest/files/power.log").readLines()

        var opponentDeckEntries = emptyList<DeckEntry>()

        hsLog.setListener(object : DefaultHSLogListener() {
            override fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>) {
                if (!isPlayer) {
                    opponentDeckEntries = deckEntries
                }
            }
        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        val zilliax = opponentDeckEntries.firstOrNull {
            it is DeckEntry.Item
                    && it.card.id == CardId.ZILLIAX
        }

        assert(zilliax != null)
    }

    @Test
    fun `secret listener is called`() {
        // https://hsreplay.net/replay/i5RvfvjFGFBpFmeoQUxFT4
        val powerLines = File("/home/martin/dev/hsdata/2019_07_21_Spex").readLines()

        hsLog.setListener(object : DefaultHSLogListener() {

            var secrets = emptyList<PossibleSecret>()
            override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
                super.onTurn(game, turn, isPlayer)
                when (turn) {
                    // Spex played VAPORIZE on turn 12
                    13 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.MIRROR_ENTITY && it.count > 0 } != null)
                    }
                    // I played a minion on turn 13 so Mirror Entity should now excluded
                    14 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.MIRROR_ENTITY && it.count == 0 } != null)
                    }
                    // I attached during turn 15 so there shouldn't be secrets anymore
                    16 -> {
                        assert(secrets.isEmpty())
                    }
                }
            }

            override fun onSecrets(possibleSecrets: List<PossibleSecret>) {
                secrets = possibleSecrets
            }
        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }
    }
}