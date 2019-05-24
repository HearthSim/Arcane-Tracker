package net.mbonnin.arcanetracker.hslog

import net.hearthsim.kotlin.hslog.PowerParser
import net.mbonnin.arcanetracker.helper.getClassIndex
import net.mbonnin.arcanetracker.hslog.achievements.AchievementsParser
import net.mbonnin.arcanetracker.hslog.loadingscreen.LoadingScreenParser
import net.mbonnin.arcanetracker.hslog.power.Game
import net.mbonnin.arcanetracker.hslog.power.GameLogic
import net.mbonnin.arcanetracker.hslog.power.GameType
import net.mbonnin.arcanetracker.hslog.util.WhizbangAndZayleHelper
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.enum.CardId

interface Console {
    fun debug(message: String)
    fun error(message: String)
    fun error(throwable: Throwable)
}

typealias DeckChangedListener = (deck: Deck) -> Unit
typealias RawGameListener = (gameStr: String, gameStart: Long) -> Unit

class HSLog(private val console: Console, private val cardJson: CardJson) {
    private val loadingScreenParser = LoadingScreenParser(console)
    private val achievementsParser = AchievementsParser(console)
    private val gameLogic = GameLogic(console, cardJson)
    private val playerDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val opponentDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val rawGameListenerList = mutableListOf<RawGameListener>()

    private val powerParser = PowerParser(
            mTagConsumer = { tag ->
                gameLogic.handleRootTag(tag)
            },
            mRawGameConsumer = { gameStr, gameStart ->
                rawGameListenerList.forEach { it(gameStr, gameStart) }
            },
            //console = console
            logger = { format, args -> console.debug(message = format) }
    )

    init {
        onGameStart {
            selectDecks(it)
        }
    }


    fun processLoadingScreen(rawLine: String, isOldData: Boolean) {
        loadingScreenParser.process(rawLine, isOldData)
    }

    fun processPower(rawLine: String, isOldData: Boolean) {
        powerParser.process(rawLine, isOldData)
    }

    fun processAchievement(rawLine: String, isOldData: Boolean) {
        achievementsParser.process(rawLine, isOldData)
    }

    fun onGameStart(block: (Game) -> Unit) {
        gameLogic.onGameStart(block)
    }

    fun whenSomethingChanges(block: (Game) -> Unit) {
        gameLogic.whenSomethingChanges(block)
    }

    fun onGameEnd(block: (Game) -> Unit) {
        gameLogic.onGameEnd(block)
    }

    fun onPlayerDeckChanged(listener: DeckChangedListener) {
        playerDeckChangedListenerList.add(listener)
    }

    fun onOpponentDeckChanged(listener: DeckChangedListener) {
        opponentDeckChangedListenerList.add(listener)
    }

    fun onRawGame(listener: RawGameListener) {
        rawGameListenerList.add(listener)
    }

    fun currentOrFinishedGame(): Game? {
        return gameLogic.currentOrFinishedGame
    }

    private fun selectDecks(game: Game) {
        val opponentDeck = Deck()
        opponentDeck.classIndex = game.opponent!!.classIndex!!

        opponentDeckChangedListenerList.forEach {
            it(opponentDeck)
        }

        var playerDeck: Deck? = null

        when (game.gameType) {
            GameType.GT_TAVERNBRAWL.name,
            GameType.GT_VS_AI.name -> {
                val emptyDeck = Deck()
                emptyDeck.name = ""
                emptyDeck.id = "rototo"
                emptyDeck.classIndex = getClassIndex(game.player!!.playerClass!!)
                playerDeck = emptyDeck
            }
        }

        if (GameLogic.isPlayerWhizbang(game)) {
            val whizbangDeck = WhizbangAndZayleHelper.findWhizbangDeck(game)

            if (whizbangDeck != null) {
                console.debug("Found whizbang deck: ${whizbangDeck.name}")
                whizbangDeck.id = "rototo"
                whizbangDeck.name = cardJson.getCard(CardId.WHIZBANG_THE_WONDERFUL).name
                playerDeck = whizbangDeck
            }
        }

        if (GameLogic.isPlayerZayle(game)) {
            val zayleDeck = WhizbangAndZayleHelper.finZayleDeck(game)

            if (zayleDeck != null) {
                console.debug("Found whizbang deck: ${zayleDeck.name}")
                zayleDeck.id = "rototo"
                zayleDeck.name = cardJson.getCard(CardId.ZAYLE_SHADOW_CLOAK).name
                playerDeck = zayleDeck
            }
        }

        if (playerDeck != null) {
            playerDeckChangedListenerList.forEach {
                it(playerDeck)
            }
        }
    }


}