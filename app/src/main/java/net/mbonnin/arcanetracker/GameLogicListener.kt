package net.mbonnin.arcanetracker

import android.os.Bundle
import android.os.Handler
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import net.mbonnin.arcanetracker.helper.DeckStringHelper
import net.mbonnin.arcanetracker.helper.WhizbangHelper
import net.mbonnin.arcanetracker.helper.getClassIndex
import net.mbonnin.arcanetracker.hsreplay.HSReplay
import net.mbonnin.arcanetracker.hsreplay.model.Lce
import net.mbonnin.arcanetracker.hsreplay.model.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
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
import net.mbonnin.arcanetracker.ui.overlay.adapter.PlayerDeckListAdapter
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.hsmodel.enum.CardId
import timber.log.Timber
import java.util.*

class GameLogicListener private constructor() : GameLogic.Listener {
    private val mHandler: Handler
    var currentGame: Game? = null

    override fun gameStarted(game: Game) {
        Timber.w("gameStarted")

        val opponentDeck = Deck()
        opponentDeck.classIndex = game.opponent!!.classIndex()
        MainViewCompanion.opponentCompanion.deck = opponentDeck

        when (game.gameType) {
            GameType.GT_TAVERNBRAWL.name,
            GameType.GT_VS_AI.name -> {
                val emptyDeck = Deck()
                emptyDeck.name = Utils.getString(R.string.deck)
                emptyDeck.id = "rototo"
                emptyDeck.classIndex = getClassIndex(game.player!!.playerClass())
                MainViewCompanion.playerCompanion.deck = emptyDeck
            }
        }

        if (!game.player!!.entity!!.tags["WHIZBANG_DECK_ID"].isNullOrBlank()) {
            val playerEntityList = game.getEntityList { entity ->
                game.player!!.entity!!.PlayerID == entity.extra.originalController
                        && entity.card != null
            }

            val whizbangDeck = WhizbangHelper.recipes
                    .asSequence()
                    .map { DeckStringHelper.parse(it) }
                    .filterNotNull()
                    .firstOrNull { deck2 ->
                        playerEntityList.filter { !deck2.cards.containsKey(it.card!!.id) }.isEmpty()
                    }

            if (whizbangDeck != null) {
                Timber.d("Found whizbang deck !")
                whizbangDeck.id = "rototo"
                whizbangDeck.name = CardUtil.getCard(CardId.WHIZBANG_THE_WONDERFUL).name
                PlayerDeckListAdapter.get().setWhizbangDeck(whizbangDeck)
                MainViewCompanion.playerCompanion.deck = whizbangDeck
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

        MainViewCompanion.playerCompanion.deck?.let { updateCounter(it.id, currentGame!!.victory) }

        sendTrackobotResult()

        FileTree.get().sync()

        val bundle = Bundle()
        bundle.putString(EventParams.GAME_TYPE.value, currentGame!!.gameType)
        bundle.putString(EventParams.FORMAT_TYPE.value, currentGame!!.formatType)
        bundle.putString(EventParams.TRACK_O_BOT.value, (Trackobot.get().currentUser() != null).toString())
        bundle.putString(EventParams.HSREPLAY.value, (HSReplay.get().token() != null).toString())
        FirebaseAnalytics.getInstance(HDTApplication.context).logEvent("game_ended", bundle)
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

            FirebaseAnalytics.getInstance(HDTApplication.context).logEvent("trackobot_upload", null)
            Trackobot.get().sendResult(resultData)
        }
    }

    private fun updateCounter(id: String, victory: Boolean) {
        val winsIncrement = if (victory) 1 else 0
        WLCounter.increment(id, winsIncrement, 1 - winsIncrement)
    }

    override fun somethingChanged() {

    }

    init {
        mHandler = Handler()

    }

    fun uploadGame(gameStr: String, gameStart: Date) {
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
        uploadRequest.match_start = Utils.ISO8601DATEFORMAT.format(gameStart)
        uploadRequest.build = HDTApplication.get().hearthstoneBuild
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

        Single.zip(insertGameSingle, hsReplaySingle, BiFunction<InsertResult, Lce<out String?>, Unit>  { insertResult, lce ->
            if (lce.error != null) {
                Timber.d(lce.error)
                Toaster.show(HDTApplication.context.getString(R.string.hsreplayError))
            } else if (lce.data != null) {
                summary.hsreplayUrl = lce.data
                GameSummary.sync()

                if (insertResult.success) {
                    RDatabaseSingleton.instance.gameDao().update(insertResult.id, lce.data)
                }

                Timber.d("hsreplay upload success")
                Toaster.show(HDTApplication.context.getString(R.string.hsreplaySuccess))
            }
        })
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    companion object {

        private var sGameLogicListener: GameLogicListener? = null
        
        fun get(): GameLogicListener {
            if (sGameLogicListener == null) {
                sGameLogicListener = GameLogicListener()
            }

            return sGameLogicListener!!
        }
    }
}
