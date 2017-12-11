package net.mbonnin.arcanetracker

import android.os.Bundle
import android.os.Handler
import com.google.firebase.analytics.FirebaseAnalytics
import net.mbonnin.arcanetracker.detector.FORMAT_STANDARD
import net.mbonnin.arcanetracker.detector.FORMAT_WILD
import net.mbonnin.arcanetracker.detector.MODE_CASUAL
import net.mbonnin.arcanetracker.detector.MODE_RANKED
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.arcanetracker.parser.LoadingScreenParser
import net.mbonnin.arcanetracker.trackobot.Trackobot
import net.mbonnin.arcanetracker.trackobot.model.CardPlay
import net.mbonnin.arcanetracker.trackobot.model.Result
import net.mbonnin.arcanetracker.trackobot.model.ResultData
import timber.log.Timber
import java.util.*

class GameLogicListener private constructor() : GameLogic.Listener {
    private val mHandler: Handler
    var currentGame: Game? = null
    private var mGameOver: Boolean = false

    override fun gameStarted(game: Game) {
        Timber.w("gameStarted")

        var deck = MainViewCompanion.getPlayerCompanion().deck
        if (Settings.get(Settings.AUTO_SELECT_DECK, true)) {
            if (LoadingScreenParser.MODE_DRAFT == LoadingScreenParser.get().gameplayMode) {
                deck = DeckList.getArenaDeck()
                Timber.w("useArena deck")
            } else {
                val classIndex = game.getPlayer().classIndex()

                /*
                 * we filter the original deck to remove the coin mainly
                 */
                val map = game.getEntityList { entity ->
                    (game.player.entity.PlayerID == entity.tags[Entity.KEY_CONTROLLER]
                            && Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]
                            && game.player.entity.PlayerID == entity.extra.originalController)
                }
                        .toCardMap()

                deck = activateBestDeck(classIndex, map)
            }
        }

        MainViewCompanion.getPlayerCompanion().deck = deck

        DeckList.getOpponentDeck().clear()
        DeckList.getOpponentDeck().classIndex = game.getOpponent().classIndex()
        MainViewCompanion.getOpponentCompanion().deck = DeckList.getOpponentDeck()

        currentGame = game
        mGameOver = false

        if (LoadingScreenParser.get().gameplayMode == LoadingScreenParser.MODE_DRAFT) {
            currentGame!!.bnetGameType = BnetGameType.BGT_ARENA
        } else if (LoadingScreenParser.get().gameplayMode == LoadingScreenParser.MODE_TAVERN_BRAWL) {
            currentGame!!.bnetGameType = BnetGameType.BGT_TAVERNBRAWL_1P_VERSUS_AI
        } else if (LoadingScreenParser.get().gameplayMode == LoadingScreenParser.MODE_ADVENTURE) {
            currentGame!!.bnetGameType = BnetGameType.BGT_VS_AI
        } else if (FMRHolder.mode == MODE_RANKED && FMRHolder.format == FORMAT_STANDARD) {
            currentGame!!.bnetGameType = BnetGameType.BGT_RANKED_STANDARD
            currentGame!!.rank = FMRHolder.rank
        } else if (FMRHolder.mode == MODE_RANKED && FMRHolder.format == FORMAT_WILD) {
            currentGame!!.bnetGameType = BnetGameType.BGT_RANKED_WILD
            currentGame!!.rank = FMRHolder.rank
        } else if (FMRHolder.mode == MODE_CASUAL && FMRHolder.format == FORMAT_STANDARD) {
            currentGame!!.bnetGameType = BnetGameType.BGT_CASUAL_STANDARD_NORMAL
        } else if (FMRHolder.mode == MODE_CASUAL && FMRHolder.format == FORMAT_WILD) {
            currentGame!!.bnetGameType = BnetGameType.BGT_CASUAL_WILD
        } else {
            currentGame!!.bnetGameType = BnetGameType.BGT_UNKNOWN
        }
    }

    override fun gameOver() {
        val mode = LoadingScreenParser.get().gameplayMode

        Timber.w("gameOver  %s [mode %s] [user %s]", if (currentGame!!.victory) "victory" else "lost", mode, Trackobot.get().currentUser())

        val deck = MainViewCompanion.getPlayerCompanion().deck

        addKnownCardsToDeck(currentGame!!, deck)

        if (currentGame!!.victory) {
            deck.wins++
        } else {
            deck.losses++
        }
        MainViewCompanion.getPlayerCompanion().deck = deck

        if (DeckList.ARENA_DECK_ID == deck.id) {
            DeckList.saveArena()
        } else {
            DeckList.save()
        }

        if ((Utils.isAppDebuggable || LoadingScreenParser.MODE_DRAFT == mode || LoadingScreenParser.MODE_TOURNAMENT == mode) && Trackobot.get().currentUser() != null) {
            val resultData = ResultData()
            resultData.result = Result()
            resultData.result.coin = currentGame!!.getPlayer().hasCoin
            resultData.result.win = currentGame!!.victory
            resultData.result.mode = Trackobot.getMode(currentGame!!.bnetGameType)
            if (currentGame!!.rank >= 0) {
                resultData.result.rank = currentGame!!.rank
            }
            resultData.result.hero = Trackobot.getHero(currentGame!!.player.classIndex())
            resultData.result.opponent = Trackobot.getHero(currentGame!!.opponent.classIndex())
            resultData.result.added = Utils.ISO8601DATEFORMAT.format(Date())

            val history = ArrayList<CardPlay>()
            for (play in currentGame!!.plays) {
                val cardPlay = CardPlay()
                cardPlay.player = if (play.isOpponent) "opponent" else "me"
                cardPlay.turn = (play.turn + 1) / 2
                cardPlay.card_id = play.cardId
                history.add(cardPlay)
            }

            resultData.result.card_history = history

            Trackobot.get().sendResult(resultData)
        }

        FileTree.get().sync()

        val bundle = Bundle()
        bundle.putString(EventParams.BNET_GAME_TYPE.value, currentGame!!.bnetGameType.name)
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("game_ended", bundle)

        mGameOver = true
    }

    private fun addKnownCardsToDeck(game: Game, deck: Deck) {

        val originalDeck = game.getEntityList { entity -> game.player.entity.PlayerID == entity.extra.originalController }
        val originalDeckMap = originalDeck.toCardMap()
        if (Settings.get(Settings.AUTO_ADD_CARDS, true) && Utils.cardMapTotal(deck.cards) < Deck.MAX_CARDS) {
            for (cardId in originalDeckMap.keys) {
                val found = originalDeckMap[cardId]!!
                if (found > Utils.cardMapGet(deck.cards, cardId)) {
                    Timber.w("adding card to the deck " + cardId)
                    deck.cards.put(cardId, found)
                }
            }
            DeckList.save()
        }

    }

    override fun somethingChanged() {

    }

    init {
        mHandler = Handler()

    }

    fun uploadGame(gameStr: String, gameStart: String) {
        val startTime = System.currentTimeMillis()

        Timber.d("ready to send hsreplay")
        val runnable = object : Runnable {
            override fun run() {
                if (mGameOver) {
                    HSReplay.get().uploadGame(gameStart, currentGame, gameStr)
                } else if (System.currentTimeMillis() - startTime < 30000) {
                    mHandler.postDelayed(this, 1000)
                } else {
                    Timber.e("timeout waiting for PowerState to finish")
                }
            }
        }

        runnable.run()
    }

    companion object {

        private var sGameLogicListener: GameLogicListener? = null

        private fun activateBestDeck(classIndex: Int, initialCards: HashMap<String, Int>): Deck {
            val deck = MainViewCompanion.getPlayerCompanion().deck
            if (deckScore(deck, classIndex, initialCards) != -1) {
                // the current deck works fine
                return deck
            }

            // sort the deck list by descending number of cards. We'll try to get the one with the most cards.
            val index = ArrayList<Int>()
            for (i in 0 until DeckList.get().size) {
                index.add(i)
            }

            Collections.sort(index) { a, b -> DeckList.get()[b!!].cardCount - DeckList.get()[a!!].cardCount }

            var maxScore = -1
            var bestDeck: Deck? = null

            for (i in index) {
                val candidateDeck = DeckList.get()[i]

                val score = deckScore(candidateDeck, classIndex, initialCards)

                Timber.i("Deck selection " + candidateDeck.name + " has score " + score)
                if (score > maxScore) {
                    bestDeck = candidateDeck
                    maxScore = score
                }
            }

            if (bestDeck == null) {
                /*
             * No good candidate, create a new deck
             */
                bestDeck = DeckList.createDeck(classIndex)
            }

            return bestDeck!!
        }

        /**
         *
         */
        private fun deckScore(deck: Deck, classIndex: Int, mulliganCards: HashMap<String, Int>): Int {
            if (deck.classIndex != classIndex) {
                return -1
            }

            var matchedCards = 0
            var newCards = 0

            /*
         * copy the cards
         */
            val deckCards = HashMap(deck.cards)

            /*
         * iterate through the mulligan cards.
         *
         * count the one that match the original deck and remove them from the original deck
         *
         * if a card is not in the original deck, increase newCards. At the end, if the total of cards is > 30, the deck is not viable
         */
            for (cardId in mulliganCards.keys) {
                val inDeck = Utils.cardMapGet(deckCards, cardId)
                val inMulligan = Utils.cardMapGet(mulliganCards, cardId)

                val a = Math.min(inDeck, inMulligan)

                Utils.cardMapAdd(deckCards, cardId, -a)
                newCards += inMulligan - a
                matchedCards += a
            }

            return if (Utils.cardMapTotal(deckCards) + matchedCards + newCards > Deck.MAX_CARDS) {
                -1
            } else matchedCards

        }

        fun get(): GameLogicListener {
            if (sGameLogicListener == null) {
                sGameLogicListener = GameLogicListener()
            }

            return sGameLogicListener!!
        }
    }
}
