package net.hearthsim.hslog

import com.soywiz.klock.DateTime
import net.hearthsim.console.Console
import net.hearthsim.hslog.parser.achievements.AchievementsParser
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.decks.DecksParser
import net.hearthsim.hslog.parser.loadingscreen.LoadingScreenParser
import net.hearthsim.hslog.parser.power.*
import net.hearthsim.hslog.util.WhizbangAndZayleHelper
import net.hearthsim.hslog.util.getClassIndex
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId


class HSLog(private val console: Console, private val cardJson: CardJson, private val debounceDelay: Long = 200) {

    private var listener: HSLogListener? = null
    private val gameLogic = GameLogic(console, cardJson)

    private val controllerPlayer = ControllerPlayer(console, cardJson)
    private val controllerOpponent = ControllerOpponent(console, cardJson)
    private val controllerBattleGrounds = ControllerBattlegrounds(console, cardJson)

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
                playerDeckChanged(deck)
            }
    )

    private val powerParser = PowerParser(
            tagConsumer = { tag ->
                gameLogic.handleRootTag(tag)
            },
            rawGameConsumer = { gameStr, gameStart ->
                listener?.onRawGame(gameStr, gameStart)
            },
            //console = console
            logger = { format, args -> console.debug(message = format) }
    )

    private var lastTime = DateTime.now().unixMillisLong
    private var lastBattlegroundState: BattlegroundState? = null
    private var lastSecrets = emptyList<PossibleSecret>()
    private var hasShownBgHeroes = false

    init {
        gameLogic.onGameStart { game ->
            selectDecks(game)

            hasShownBgHeroes = false

            listener?.onGameStart(game)
        }

        gameLogic.onBgHeroes { game ->
            val entities = game.getEntityList {
                it.extra.isBgHeroChoice
            }.sortedBy {
                it.tags[Entity.KEY_ZONE_POSITION]
            }

            listener?.bgHeroesShow(game, entities)
        }

        gameLogic.whenSomethingChanges { game ->
            listener?.onGameChanged(game)

            if (game.gameType == GameType.GT_BATTLEGROUNDS) {
                val state = game.battlegroundState
                if (state != lastBattlegroundState) {
                    listener?.onDeckEntries(game, false, controllerBattleGrounds.getDeckEntries(game, state))
                    lastBattlegroundState = state
                }

                if (hasShownBgHeroes) {
                    listener?.bgHeroesHide()
                    hasShownBgHeroes = false
                }
            } else {
                /**
                 * This is not perfect as we might lose the very last last events in a game but it help debouncing the callbacks
                 */
                if (DateTime.now().unixMillisLong - lastTime >= debounceDelay) {
                    listener?.onDeckEntries(game, true, controllerPlayer.getDeckEntries(game))
                    listener?.onDeckEntries(game, false, controllerOpponent.getDeckEntries(game))

                    val secrets = gameLogic.getSecrets(game)
                    if (secrets != lastSecrets) {
                        listener?.onSecrets(secrets)
                        lastSecrets = secrets
                    }
                    lastTime = DateTime.now().unixMillisLong
                }

            }
        }

        gameLogic.onGameEnd { game ->
            listener?.onGameEnd(game)
        }
        gameLogic.onTurn { game, turn, isPlayer ->
            listener?.onTurn(game, turn, isPlayer)
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

    /**
     * This is called when either:
     *  * a deck was read from Decks.log
     *  * a Zayle or Whizbang deck is detected at the start of a game
     *  * a solo game has started an we set an empty game for those
     */
    private fun playerDeckChanged(deck: Deck) {
        listener?.onPlayerDeckChanged(deck)
        controllerPlayer.playerCardMap = deck.cards
    }

    private fun selectDecks(game: Game) {
        val opponentclassIndex = game.opponent!!.classIndex!!

        listener?.onOpponentDeckChanged(Deck.create(cards = emptyMap(), classIndex = opponentclassIndex, cardJson = cardJson))

        var playerDeck: Deck? = null

        when (game.gameType) {
            GameType.GT_TAVERNBRAWL,
            GameType.GT_VS_AI -> {
                val emptyDeck = Deck.create(
                        cards = emptyMap(),
                        classIndex = getClassIndex(game.player!!.playerClass!!),
                        name = "",
                        id = "rototo",
                        cardJson = cardJson)
                /**
                 * This also resets the deck for Innkeeper modes but there's no easy way to distinguish
                 * Solo vs Innkeeper:
                 *  - Decks.log: "Finding Game With Deck:"
                 *  - GameType=GT_VS_AI ScenarioID=261/256/259
                 * Dalaran Heist:
                 *  - Decks.log: "Finding Game With Hero: 54553"
                 *  - GameType=GT_VS_AI ScenarioID=3005
                 */
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
            playerDeckChanged(playerDeck)
        }
    }

    fun setListener(listener: HSLogListener) {
        this.listener = listener
    }

    /**
     * Called when the user switches decks manually
     */
    fun setDeck(deck: Deck) {
        playerDeckChanged(deck)
    }
}