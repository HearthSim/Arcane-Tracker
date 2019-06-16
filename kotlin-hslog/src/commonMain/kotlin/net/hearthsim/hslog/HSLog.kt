package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.PowerParser
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hslog.parser.achievements.AchievementsParser
import net.hearthsim.hslog.parser.decks.DecksParser
import net.hearthsim.hslog.parser.loadingscreen.LoadingScreenParser
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.power.GameLogic
import net.hearthsim.hslog.power.GameType
import net.hearthsim.hslog.util.WhizbangAndZayleHelper
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId


class HSLog(private val console: Console, private val cardJson: CardJson) {
    interface Listener {
        /**
         * called when a game starts, just after the mulligan
         * @param game: the game
         */
        fun onGameStart(game: Game)

        /**
         * called when a new entity has been revealed, a play has been done or something else changed
         * @param game: the game
         */
        fun onGameChanged(game: Game)

        /**
         * called when the game ends
         * @param game: the game
         */
        fun onGameEnd(game: Game)

        /**
         *
         */
        fun onRawGame(gameString: String, gameStartMillis: Long)

        /**
         *
         */
        fun onCardGained(cardGained: AchievementsParser.CardGained)

        /**
         *
         */
        fun onDeckFound(deck: Deck, deckString: String, isArena: Boolean)

        /**
         * called when the player deck is detected
         *      - from Decks.log in ranked/casual/arena/practice
         *      - guessed from the initial mulligan cards in the Zayle/Whizbang cases
         *      - empty for adventures/dungeons and other solo modes
         * @param deck: the new deck
         */
        fun onPlayerDeckChanged(deck: Deck)

        /**
         * called to reset the opponent deck to an empty deck at the start of a game
         * @param deck: the new deck
         */
        fun onOpponentDeckChanged(deck: Deck)
    }

    private var listener: Listener? = null
    private val gameLogic = GameLogic(console, cardJson)

    private val loadingScreenParser = LoadingScreenParser(console)
    private val achievementsParser = AchievementsParser(console,
            onCard = { cardGained ->
                listener?.onCardGained(cardGained)
            }
    )
    private val decksParser = DecksParser(
            console = console,
            cardJson = cardJson,
            onNewDeckFound = { deck, deckstring, isArena ->
                listener?.onDeckFound(deck, deckstring, isArena)
            },
            onPlayerDeckChanged = { deck ->
                listener?.onPlayerDeckChanged(deck)
            }
    )
    private val powerParser = PowerParser(
            mTagConsumer = { tag ->
                gameLogic.handleRootTag(tag)
            },
            mRawGameConsumer = { gameStr, gameStart ->
                listener?.onRawGame(gameStr, gameStart)
            },
            //console = console
            logger = { format, args -> console.debug(message = format) }
    )

    init {
        gameLogic.onGameStart {game ->
            selectDecks(game)
            listener?.onGameStart(game)
        }
        gameLogic.whenSomethingChanges {game ->
            listener?.onGameChanged(game)
        }
        gameLogic.onGameEnd {game ->
            listener?.onGameEnd(game)
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

    fun currentOrFinishedGame(): Game? {
        return gameLogic.currentOrFinishedGame
    }

    private fun selectDecks(game: Game) {
        val opponentclassIndex = game.opponent!!.classIndex!!

        listener?.onOpponentDeckChanged(Deck.create(cards = emptyMap(), classIndex = opponentclassIndex, cardJson = cardJson))

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
            listener?.onPlayerDeckChanged(playerDeck)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}