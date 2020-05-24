package net.hearthsim.hslog

import net.hearthsim.hslog.TestUtils.testFile
import net.hearthsim.hslog.parser.power.BattlegroundState
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.PossibleSecret
import net.hearthsim.hsmodel.enum.CardId
import org.junit.Test
import java.io.File

class PowerParserTest {
    val cardJson = TestUtils.cardJson

    val hsLog = TestUtils.newHSLog()

    @Test
    fun `magnetized minions appear in the opponent deck`() {
        val powerLines = testFile("power.log").readLines()

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
    fun `vaporize test`() {
        // https://hsreplay.net/replay/i5RvfvjFGFBpFmeoQUxFT4
        val powerLines = testFile("2019_07_21_Spex").readLines()

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
                        assert(secrets.firstOrNull { it.cardId == CardId.MIRROR_ENTITY }?.count == 0)
                    }
                    // I attacked the hero during turn 15 so secret should have triggered
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

    @Test
    fun `pressure plate test`() {
        // https://hsreplay.net/replay/i5RvfvjFGFBpFmeoQUxFT4
        val powerLines = testFile("2019_08_10_Glorfindel").readLines()

        hsLog.setListener(object : DefaultHSLogListener() {

            var secrets = emptyList<PossibleSecret>()
            override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
                super.onTurn(game, turn, isPlayer)
                when (turn) {
                    // Opponent played PRESSURE_PLATE on turn 4
                    5 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.PRESSURE_PLATE && it.count > 0 } != null)
                    }
                    // I attacked the secret keeper on turn 5 so both freezing trap and snake trap should be excluded
                    6 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.SNAKE_TRAP }?.count == 0)
                        assert(secrets.firstOrNull { it.cardId == CardId.FREEZING_TRAP }?.count == 0)
                    }
                    // I played prismatic lens on turn 7 so secret should have triggered
                    8 -> {
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

    @Test
    fun `pressure plate test2`() {
        // https://hsreplay.net/replay/bDVPN8buJHt9apsVXKSDSo
        val powerLines = testFile("2019_08_11_MrDude").readLines()

        hsLog.setListener(object : DefaultHSLogListener() {

            var secrets = emptyList<PossibleSecret>()
            override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
                super.onTurn(game, turn, isPlayer)
                when (turn) {
                    // Opponent played PRESSURE_PLATE on turn 4
                    5 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.PRESSURE_PLATE }?.count ?: 0 > 0)
                    }
                    // I played prismatic lens on turn 11 with an empty board so it should not exclude PRESSURE_PLATE
                    12 -> {
                        assert(secrets.firstOrNull { it.cardId == CardId.PRESSURE_PLATE }?.count ?: 0 > 0)
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

    @Test
    fun `bombs are shown in opponent deck`() {
        // https://hsreplay.net/replay/6Wfwiptda9QLiascu9tSJ6
        val powerLines = testFile("2019_08_11_sofa").readLines()

        var opponentDeckEntries = emptyList<DeckEntry>()

        hsLog.setListener(object : DefaultHSLogListener() {
            override fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>) {
                if (!isPlayer) {
                    opponentDeckEntries = deckEntries
                }
            }

            override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
                super.onTurn(game, turn, isPlayer)
                if (turn == 30) {
                    println("Bombs have been placed")
                }
            }
        })

        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        val bombEntry = opponentDeckEntries.firstOrNull {
            it is DeckEntry.Item
                    && it.card.id == CardId.BOMB
        }

        assert((bombEntry is DeckEntry.Item) && bombEntry.count == 2)
    }

    @Test
    fun `battlegrounds games are correctly parsed`() {
        //val powerLines = File("$home/dev/hsdata/2019_11_11_battlegrounds").readLines()
        val powerLines = testFile("2019_11_17_01-01_battlegrounds").readLines()
        val hsLog = TestUtils.newHSLog()

        var gameAtStart: Game? = null
        var gameAtEnd: Game? = null

        var lastState: BattlegroundState? = null
        hsLog.setListener(object : DefaultHSLogListener() {

            override fun onGameStart(game: Game) {
                super.onGameStart(game)
                gameAtStart = game
            }

            override fun onGameChanged(game: Game) {
                super.onGameChanged(game)

                val newState = game.battlegroundState
                if (newState != lastState) {
                    println("----------------------------------------")
                    println("--------------Turn ${game.gameEntity?.tags?.get(Entity.KEY_TURN)}-------------------")
                    println("----------------------------------------")
                    newState.boards.forEach {
                        TestUtils.console.debug(it.toString())
                    }
                    lastState = newState
                }
            }


            override fun onGameEnd(game: Game) {
                super.onGameEnd(game)
                gameAtEnd = game
            }
        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        assert(lastState!!.boards[0].heroCardId == CardId.KING_MUKLA1)
        assert(lastState!!.boards[1].heroCardId == CardId.BARTENDOTRON1)
        assert(lastState!!.boards[2].heroCardId == CardId.PATCHWERK2)
        assert(lastState!!.boards[3].heroCardId == CardId.LICH_BAZHIAL)
        assert(lastState!!.boards[4].heroCardId == CardId.YOGGSARON_HOPES_END1)

        assert(gameAtStart != null)
        assert(gameAtEnd != null)
    }

    @Test
    fun `battlegrounds heroes are correctly detected`() {
        //val powerLines = File("$home/dev/hsdata/2019_11_11_battlegrounds").readLines()
        val powerLines = testFile("2019_11_17_18-26_battlegrounds").readLines()
        val hsLog = TestUtils.newHSLog()

        val foundEntities = mutableListOf<Entity>()
        var heroesHaveBeenHidden = false

        hsLog.setListener(object : DefaultHSLogListener() {
            override fun bgHeroesShow(game: Game, entities: List<Entity>) {
                foundEntities.addAll(entities)
            }

            override fun bgHeroesHide() {
                heroesHaveBeenHidden = true
            }
        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        assert(heroesHaveBeenHidden)
        assert(foundEntities[0].CardID == CardId.THE_RAT_KING1)
        assert(foundEntities[1].CardID == CardId.LORD_JARAXXUS2)
        assert(foundEntities[2].CardID == CardId.QUEEN_WAGTOGGLE1)
    }

    fun balanceBlocks() {
        val dir = File("/dev/hsdata")

        val map = mutableMapOf<String, Int>()

        dir.listFiles().filter {
            !it.name.contains("battleground")
                    && !it.name.contains("wisersheis")
        }.forEach {
            if (it.isFile) {
                balance(it, map)
            }
        }

        println("TOTAL")
        map.entries.sortedBy { it.key }.forEach {
            System.out.printf(      "%30.30s: %4d\n",
                    it.key,
                    it.value
            )
        }
    }

    private fun balance(file: File, blockTypes: MutableMap<String, Int>) {
        val lines = file.readLines()

        var BLOCK_START = 0
        var BLOCK_END = 0
        var BlockStart = 0
        var BlockEnd = 0

        var depth = 0
        val map = mutableMapOf<String, Int>()

        lines.filter { it.contains("PowerTaskList.DebugPrintPower") }.forEach {
            val m = Regex(".*BLOCK_START BlockType=([^ ]*) .*").matchEntire(it)
            if (m != null) {
                map.merge(m.groupValues[1] + depth.toString(), 1) { old, new ->
                    old + 1
                }
                depth++
                BLOCK_START++
            }
            if (it.contains("BLOCK_END")) {
                depth--
                BLOCK_END++
            }
            if (it.contains("Block Start")) {
                BlockStart++
            }
            if (it.contains("Block End")) {
                BlockEnd++
            }
        }

        System.out.printf("%30.30s: BLOCK_START/BLOCK_END: %4d - %4d = %4d    BlockStart/BlockEnd: %4d - %4d = %4d    Sum: %4d - %4d = %4d\n",
                file.name,
                BLOCK_START, BLOCK_END, BLOCK_START - BLOCK_END,
                BlockStart, BlockEnd, BlockStart - BlockEnd,
                BLOCK_START + BlockStart, BLOCK_END + BlockEnd, BLOCK_START + BlockStart - BLOCK_END - BlockEnd
                )

        map.entries.sortedBy { it.key }.forEach {
            System.out.printf(      "%30.30s: %4d\n",
                    it.key,
                    it.value
            )
        }
        blockTypes.mergeReduceInPlace(map) { a, b ->
            a + b
        }
    }
}

fun <K, V> MutableMap<K, V>.mergeReduceInPlace(vararg others: Map<K, V>, reduce: (V, V) -> V) =
        others.forEach { other -> other.forEach { merge(it.key, it.value, reduce) } }