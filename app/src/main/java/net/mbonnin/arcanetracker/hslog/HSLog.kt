package net.mbonnin.arcanetracker.hslog

import net.hearthsim.kotlin.hslog.PowerParser
import net.mbonnin.arcanetracker.GameType
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.WhizbangAndZayleHelper
import net.mbonnin.arcanetracker.helper.getClassIndex
import net.mbonnin.arcanetracker.ui.overlay.adapter.PlayerDeckListAdapter
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.enum.CardId
import timber.log.Timber

interface Console {
    fun debug(message: String)
    fun error(message: String)
    fun error(throwable: Throwable)
}

typealias DeckChangedListener = (deck: Deck) -> Unit
typealias RawGameListener = (gameStr: String, gameStart: Long) -> Unit

class HSLog(private val console: Console, private val cardJson: CardJson) {
    private val loadingScreenParser = LoadingScreenParser()
    private val gameLogic = GameLogic(console)
    private val playerDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val opponentDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val rawGameListenerList = mutableListOf<RawGameListener>()

    val powerParser = PowerParser(
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
        opponentDeck.classIndex = game.opponent!!.classIndex()

        opponentDeckChangedListenerList.forEach {
            it(opponentDeck)
        }

        MainViewCompanion.opponentCompanion.deck = opponentDeck

        var playerDeck: Deck? = null

        when (game.gameType) {
            GameType.GT_TAVERNBRAWL.name,
            GameType.GT_VS_AI.name -> {
                val emptyDeck = Deck()
                emptyDeck.name = Utils.getString(R.string.deck)
                emptyDeck.id = "rototo"
                emptyDeck.classIndex = getClassIndex(game.player!!.playerClass())
                playerDeck = emptyDeck
            }
        }

        if (GameLogic.isPlayerWhizbang(game)) {
            val whizbangDeck = WhizbangAndZayleHelper.findWhizbangDeck(game)

            if (whizbangDeck != null) {
                Timber.d("Found whizbang deck: ${whizbangDeck.name}")
                whizbangDeck.id = "rototo"
                whizbangDeck.name = cardJson.getCard(CardId.WHIZBANG_THE_WONDERFUL).name
                PlayerDeckListAdapter.get().setWhizbangDeck(whizbangDeck)
                playerDeck = whizbangDeck
            }
        }

        if (GameLogic.isPlayerZayle(game)) {
            val zayleDeck = WhizbangAndZayleHelper.finZayleDeck(game)

            if (zayleDeck != null) {
                Timber.d("Found whizbang deck: ${zayleDeck.name}")
                zayleDeck.id = "rototo"
                zayleDeck.name = cardJson.getCard(CardId.ZAYLE_SHADOW_CLOAK).name
                PlayerDeckListAdapter.get().setZayleDeck(zayleDeck)
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