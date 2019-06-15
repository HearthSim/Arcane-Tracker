package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.PowerParser
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hslog.achievements.AchievementsParser
import net.hearthsim.hslog.decks.DecksParser
import net.hearthsim.hslog.loadingscreen.LoadingScreenParser
import net.hearthsim.hslog.power.Game
import net.hearthsim.hslog.power.GameLogic
import net.hearthsim.hslog.power.GameType
import net.hearthsim.hslog.util.WhizbangAndZayleHelper
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId

interface Console {
    fun debug(message: String)
    fun error(message: String)
    fun error(throwable: Throwable)
}

typealias DeckChangedListener = (deck: Deck) -> Unit
typealias RawGameListener = (gameStr: String, gameStart: Long) -> Unit
typealias CardGainedListener = (cardGained: AchievementsParser.CardGained) -> Unit
typealias NewDeckFoundListener = (deck: Deck, deckString: String, isArena: Boolean) -> Unit

class HSLog(private val console: Console, private val cardJson: CardJson) {
    private val gameLogic = GameLogic(console, cardJson)
    private val playerDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val opponentDeckChangedListenerList = mutableListOf<DeckChangedListener>()
    private val rawGameListenerList = mutableListOf<RawGameListener>()
    private val cardGainedListenerList = mutableListOf<CardGainedListener>()
    private val newDeckFoundListenerList = mutableListOf<NewDeckFoundListener>()

    private val loadingScreenParser = LoadingScreenParser(console)
    private val achievementsParser = AchievementsParser(console,
            onCard = { cardGained ->
                cardGainedListenerList.forEach {
                    it(cardGained)
                }
            }
    )
    private val decksParser = DecksParser(
            console = console,
            cardJson = cardJson,
            onNewDeckFound = { deck, deckstring, isArena ->
                newDeckFoundListenerList.forEach {
                    it(deck, deckstring, isArena)
                }
            },
            onPlayerDeckChanged = { deck ->
                playerDeckChangedListenerList.forEach {
                    it(deck)
                }
            }
    )
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

    fun processDecks(rawLine: String, isOldData: Boolean) {
        decksParser.process(rawLine, isOldData)
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

    fun onCardGained(listener: CardGainedListener) {
        cardGainedListenerList.add(listener)
    }

    fun onNewDeckFound(listener: NewDeckFoundListener) {
        newDeckFoundListenerList.add(listener)
    }

    fun currentOrFinishedGame(): Game? {
        return gameLogic.currentOrFinishedGame
    }

    private fun selectDecks(game: Game) {
        val opponentclassIndex = game.opponent!!.classIndex!!

        opponentDeckChangedListenerList.forEach {
            it(Deck.create(cards = emptyMap(), classIndex = opponentclassIndex, cardJson = cardJson))
        }

        var playerDeck: Deck? = null

        when (game.gameType) {
            GameType.GT_TAVERNBRAWL.name,
            GameType.GT_VS_AI.name -> {
                val emptyDeck = Deck.create(
                        cards = emptyMap(),
                        classIndex = getClassIndex(game.player!!.playerClass!!),
                        name = "",
                        id = "rototo",
                        cardJson = cardJson)
                playerDeck = emptyDeck
            }
        }

        if (GameLogic.isPlayerWhizbang(game)) {
            val whizbangDeck = WhizbangAndZayleHelper.findWhizbangDeck(game, cardJson)

            if (whizbangDeck != null) {
                console.debug("Found whizbang deck: ${whizbangDeck.name}")
                whizbangDeck.id = "rototo"
                whizbangDeck.name = cardJson.getCard(CardId.WHIZBANG_THE_WONDERFUL).name
                playerDeck = whizbangDeck
            }
        }

        if (GameLogic.isPlayerZayle(game)) {
            val zayleDeck = WhizbangAndZayleHelper.finZayleDeck(game, cardJson)

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