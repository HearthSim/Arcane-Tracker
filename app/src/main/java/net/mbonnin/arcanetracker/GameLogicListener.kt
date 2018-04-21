package net.mbonnin.arcanetracker

import android.os.Bundle
import android.os.Handler
import com.google.firebase.analytics.FirebaseAnalytics
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.model.Lce
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.arcanetracker.parser.LoadingScreenParser
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RGame
import net.mbonnin.arcanetracker.room.WLCounter
import net.mbonnin.arcanetracker.trackobot.Trackobot
import net.mbonnin.arcanetracker.trackobot.model.CardPlay
import net.mbonnin.arcanetracker.trackobot.model.Result
import net.mbonnin.arcanetracker.trackobot.model.ResultData
import rx.Single
import rx.schedulers.Schedulers
import timber.log.Timber
import java.util.*

class GameLogicListener private constructor() : GameLogic.Listener {
    private val mHandler: Handler
    var currentGame: Game? = null

    override fun gameStarted(game: Game) {
        Timber.w("gameStarted")

        var deck = MainViewCompanion.legacyCompanion.deck
        if (Settings.get(Settings.AUTO_SELECT_DECK, true)) {
            if (LoadingScreenParser.MODE_DRAFT == LoadingScreenParser.get().gameplayMode) {
                deck = LegacyDeckList.arenaDeck
                Timber.w("useArena deck")
            } else {
                val classIndex = game.player!!.classIndex()

                /*
                 * we filter the original deck to remove the coin mainly
                 */
                val map = game.getEntityList { entity ->
                    game.player!!.entity!!.PlayerID == entity.tags[Entity.KEY_CONTROLLER]
                            && Entity.ZONE_HAND == entity.tags[Entity.KEY_ZONE]
                            && game.player!!.entity!!.PlayerID == entity.extra.originalController
                }
                        .toCardMap()

                deck = activateBestDeck(classIndex, map)
            }
        }

        MainViewCompanion.legacyCompanion.deck = deck

        LegacyDeckList.opponentDeck.clear()
        LegacyDeckList.opponentDeck.classIndex = game.opponent!!.classIndex()
        MainViewCompanion.opponentCompanion.deck = LegacyDeckList.opponentDeck


        when (game.gameType) {
            GameType.GT_TAVERNBRAWL.name,
            GameType.GT_VS_AI.name ->{
                val emptyDeck = Deck()
                emptyDeck.name = Utils.getString(R.string.deck)
                emptyDeck.id = "rototo"
                emptyDeck.classIndex = getClassIndex(game.player!!.playerClass())
                MainViewCompanion.playerCompanion.deck = emptyDeck
            }
        }

        currentGame = game
    }

    override fun gameOver() {
        val mode = LoadingScreenParser.get().gameplayMode

        currentGame!!.playerRank = RankHolder.playerRank
        currentGame!!.opponentRank = RankHolder.opponentRank

        RankHolder.reset()

        Timber.w("gameOver  %s [gameType %s][format_type %s][mode %s] [user %s]",
                if (currentGame!!.victory) "victory" else "lost",
                currentGame!!.gameType,
                currentGame!!.formatType,
                mode,
                Trackobot.get().currentUser())

        legacyAddCardsToDeck()

        MainViewCompanion.playerCompanion.deck?.let { updateCounter(it.id, currentGame!!.victory) }

        sendTrackobotResult()

        FileTree.get().sync()

        val bundle = Bundle()
        bundle.putString(EventParams.GAME_TYPE.value, currentGame!!.gameType)
        bundle.putString(EventParams.FORMAT_TYPE.value, currentGame!!.formatType)
        bundle.putString(EventParams.TRACK_O_BOT.value, (Trackobot.get().currentUser() != null).toString())
        bundle.putString(EventParams.HSREPLAY.value, (HSReplay.get().token() != null).toString())
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("game_ended", bundle)
    }

    class InsertResult(val id: Long, val success: Boolean)

    private fun insertGame(game: Game): Single<InsertResult> {
        val deck = MainViewCompanion.playerCompanion.deck
        if (deck == null) {
            return Single.just(InsertResult(-1L, false))
        }

        val rgame = RGame(
                deck_id = deck.id,
                victory = game.victory,
                coin = game.player!!.hasCoin,
                player_class = game.player!!.playerClass(),
                opponent_class = game.opponent!!.playerClass(),
                date = System.currentTimeMillis(),
                format_type = game.formatType!!,
                game_type = game.gameType!!,
                rank = game.playerRank,
                deck_name = deck.name
        )

        return Single.fromCallable {
            InsertResult(RDatabaseSingleton.instance.gameDao().insert(rgame), true)
        }
    }

    private fun sendTrackobotResult() {
        val mode = LoadingScreenParser.get().gameplayMode

        if ((Utils.isAppDebuggable || LoadingScreenParser.MODE_DRAFT == mode || LoadingScreenParser.MODE_TOURNAMENT == mode) && Trackobot.get().currentUser() != null) {
            val resultData = ResultData()
            resultData.result = Result()
            resultData.result.coin = currentGame!!.player!!.hasCoin
            resultData.result.win = currentGame!!.victory
            resultData.result.mode = Trackobot.getMode(currentGame!!.gameType!!)

            val playerRank = currentGame!!.playerRank
            if (playerRank != RANK_UNKNOWN) {
                resultData.result.rank = playerRank
            }
            resultData.result.hero = Trackobot.getHero(currentGame!!.player!!.classIndex())
            resultData.result.opponent = Trackobot.getHero(currentGame!!.opponent!!.classIndex())
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

            FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("trackobot_upload", null)
            Trackobot.get().sendResult(resultData)
        }
    }

    private fun legacyAddCardsToDeck() {
        val legacyDeck = MainViewCompanion.legacyCompanion.deck

        if (legacyDeck != null) {
            if (LegacyDeckList.hasValidDeck()) {
                addKnownCardsToDeck(currentGame!!, legacyDeck)
            }

            if (currentGame!!.victory) {
                legacyDeck.wins++
            } else {
                legacyDeck.losses++
            }
            MainViewCompanion.legacyCompanion.deck = legacyDeck

            if (LegacyDeckList.ARENA_DECK_ID == legacyDeck.id) {
                LegacyDeckList.saveArena()
            } else {
                LegacyDeckList.save()
            }

        }
    }

    private fun updateCounter(id: String, victory: Boolean) {
        val winsIncrement = if (victory) 1 else 0
        WLCounter.increment(id, winsIncrement, 1 - winsIncrement)
    }

    private fun addKnownCardsToDeck(game: Game, deck: Deck) {

        val originalDeck = game.getEntityList { entity -> game.player!!.entity!!.PlayerID == entity.extra.originalController }
        val originalDeckMap = originalDeck.toCardMap()
        if (Settings.get(Settings.AUTO_ADD_CARDS, true) && Utils.cardMapTotal(deck.cards) < Deck.MAX_CARDS) {
            for (cardId in originalDeckMap.keys) {
                val found = originalDeckMap[cardId]!!
                if (found > Utils.cardMapGet(deck.cards, cardId)) {
                    Timber.w("adding card to the deck " + cardId)
                    deck.cards.put(cardId, found)
                }
            }
            LegacyDeckList.save()
        }

    }

    override fun somethingChanged() {

    }

    init {
        mHandler = Handler()

    }

    fun uploadGame(gameStr: String, gameStart: String) {
        val summary = GameSummary()
        val game = currentGame

        if (game == null) {
            return
        } else if (game.spectator) {
            return
        }

        Timber.d("ready to send hsreplay %s", game.gameType)

        /*when (game.bnetGameType) {
            BnetGameType.BGT_ARENA,
            BnetGameType.BGT_CASUAL_STANDARD_NEWBIE,
            BnetGameType.BGT_CASUAL_WILD,
            BnetGameType.BGT_CASUAL_STANDARD_NORMAL,
            BnetGameType.BGT_FRIENDS,
            BnetGameType.BGT_RANKED_STANDARD,
            BnetGameType.BGT_RANKED_WILD,
            BnetGameType.BGT_VS_AI -> Unit // Note that this will never happen because there's just one GOLD_REWARD_STATE in AI mode
            else -> return // do not send strange games to HSReplay
        }*/

        summary.coin = game.player!!.hasCoin
        summary.win = game.victory
        summary.hero = game.player!!.classIndex()
        summary.opponentHero = game.opponent!!.classIndex()
        summary.date = Utils.ISO8601DATEFORMAT.format(Date())
        summary.deckName = MainViewCompanion.playerCompanion.deck?.name

        GameSummary.addFirst(summary)

        val uploadRequest = UploadRequest()
        uploadRequest.match_start = gameStart
        uploadRequest.build = ArcaneTrackerApplication.get().hearthstoneBuild
        uploadRequest.spectator_mode = game.spectator
        uploadRequest.friendly_player = game.player!!.entity!!.PlayerID
        uploadRequest.format = fromFormatTypeString(game.formatType).intValue
        uploadRequest.game_type = fromGameAndFormat(game.gameType, game.formatType!!).intValue

        val player = if (uploadRequest.friendly_player == "1") uploadRequest.player1 else uploadRequest.player2
        val opponent = if (uploadRequest.friendly_player == "1") uploadRequest.player2 else uploadRequest.player1

        val playerRank = game.playerRank
        if (playerRank != RANK_UNKNOWN) {
            player.rank = playerRank
        }

        val opponentRank = RankHolder.opponentRank
        if (opponentRank != RANK_UNKNOWN) {
            opponent.rank = opponentRank
        }

        MainViewCompanion.playerCompanion.deck?.id?.toLongOrNull()?.let {
            player.deck_id = it
        }

        MainViewCompanion.playerCompanion.deck?.let {
            it.cards.forEach {
                for (i in 0 until it.value) {
                    player.deck.add(it.key)
                }
            }
        }

        val insertGameSingle = insertGame(game)

        val hsReplaySingle = if (HSReplay.get().token() != null) {
            HSReplay.get().uploadGame(uploadRequest, gameStr)
        } else {
            Single.just(Lce.data(null))
        }

        Single.zip(insertGameSingle, hsReplaySingle) { insertResult, lce ->
            if (lce.error != null) {
                Timber.d(lce.error)
                Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplayError))
            } else if (lce.data != null) {
                summary.hsreplayUrl = lce.data
                GameSummary.sync()

                if (insertResult.success) {
                    RDatabaseSingleton.instance.gameDao().update(insertResult.id, lce.data)
                }

                Timber.d("hsreplay upload success")
                Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplaySuccess))
            }
        }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    companion object {

        private var sGameLogicListener: GameLogicListener? = null

        private fun activateBestDeck(classIndex: Int, initialCards: HashMap<String, Int>): Deck {
            val deck = MainViewCompanion.legacyCompanion.deck!!
            if (deckScore(deck, classIndex, initialCards) != -1) {
                // the current deck works fine
                return deck
            }

            // sort the deck list by descending number of cards. We'll try to get the one with the most cards.
            val index = ArrayList<Int>()
            for (i in 0 until LegacyDeckList.get().size) {
                index.add(i)
            }

            Collections.sort(index) { a, b -> LegacyDeckList.get()[b!!].cardCount - LegacyDeckList.get()[a!!].cardCount }

            var maxScore = -1
            var bestDeck: Deck? = null

            for (i in index) {
                val candidateDeck = LegacyDeckList.get()[i]

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
                bestDeck = LegacyDeckList.createDeck(classIndex)
            }

            return bestDeck
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
