package net.mbonnin.arcanetracker

import android.os.Bundle
import android.os.Handler
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import net.mbonnin.arcanetracker.helper.WhizbangAndZayleHelper
import net.mbonnin.arcanetracker.helper.getClassIndex
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RGame
import net.mbonnin.arcanetracker.room.WLCounter
import net.mbonnin.arcanetracker.ui.overlay.adapter.PlayerDeckListAdapter
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.enum.CardId
import timber.log.Timber
import java.util.*

class GameLogicListener(val cardJson: CardJson) : GameLogic.Listener {
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

        if (isPlayerWhizbang(game)) {
            val whizbangDeck = WhizbangAndZayleHelper.findWhizbangDeck(game)

            if (whizbangDeck != null) {
                Timber.d("Found whizbang deck: ${whizbangDeck.name}")
                whizbangDeck.id = "rototo"
                whizbangDeck.name = cardJson.getCard(CardId.WHIZBANG_THE_WONDERFUL).name
                PlayerDeckListAdapter.get().setWhizbangDeck(whizbangDeck)
                MainViewCompanion.playerCompanion.deck = whizbangDeck
            }
        }

        if (isPlayerZayle(game)) {
            val zayleDeck = WhizbangAndZayleHelper.finZayleDeck(game)

            if (zayleDeck != null) {
                Timber.d("Found whizbang deck: ${zayleDeck.name}")
                zayleDeck.id = "rototo"
                zayleDeck.name = cardJson.getCard(CardId.ZAYLE_SHADOW_CLOAK).name
                PlayerDeckListAdapter.get().setZayleDeck(zayleDeck)
                MainViewCompanion.playerCompanion.deck = zayleDeck
            }
        }
        currentGame = game
    }

    override fun gameOver() {
        val mode = ArcaneTrackerApplication.get().hsLog.loadingScreenMode()

        currentGame!!.playerRank = RankHolder.playerRank
        currentGame!!.opponentRank = RankHolder.opponentRank

        RankHolder.reset()

        Timber.w("gameOver  %s [gameType %s][format_type %s][mode %s]",
                if (currentGame!!.victory) "victory" else "lost",
                currentGame!!.gameType,
                currentGame!!.formatType,
                mode)

        MainViewCompanion.playerCompanion.deck?.let { updateCounter(it.id, currentGame!!.victory) }

        FileTree.get().sync()

        val bundle = Bundle()
        bundle.putString(EventParams.GAME_TYPE.value, currentGame!!.gameType)
        bundle.putString(EventParams.FORMAT_TYPE.value, currentGame!!.formatType)
        bundle.putString(EventParams.HSREPLAY.value, (ArcaneTrackerApplication.get().hsReplay.token() != null).toString())
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("game_ended", bundle)
    }

    class InsertResult(val id: Long, val success: Boolean)

    private suspend fun insertGame(game: Game): InsertResult {
        val deck = MainViewCompanion.playerCompanion.deck
        if (deck == null) {
            return InsertResult(-1L, false)
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

        return withContext(Dispatchers.IO) {
            try {
                val id = RDatabaseSingleton.instance.gameDao().insert(rgame)
                InsertResult(id, true)
            } catch (e: Exception) {
                InsertResult(-1, false)
            }
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

        GlobalScope.launch(Dispatchers.Main) {
            val insertResult = insertGame(game)

            if (ArcaneTrackerApplication.get().hsReplay.token() != null) {
                FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("hsreplay_upload", null)
                val result = ArcaneTrackerApplication.get().hsReplay.uploadGame(uploadRequest, gameStr)
                result.fold(
                        onSuccess = {
                            summary.hsreplayUrl = it
                            GameSummary.sync()

                            if (insertResult.success) {
                                withContext(Dispatchers.IO) {
                                    RDatabaseSingleton.instance.gameDao().update(insertResult.id, it)
                                }
                            }

                            Timber.d("hsreplay upload success")
                            Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplaySuccess))
                        },
                        onFailure = {
                            Timber.d(result.exceptionOrNull())
                            Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplayError))
                        }
                )
            }

        }
    }

    companion object {
        fun isPlayerWhizbang(game: Game): Boolean {
            return !game.player!!.entity!!.tags["WHIZBANG_DECK_ID"].isNullOrBlank()
        }

        fun isPlayerZayle(game: Game): Boolean {
            return game.getEntityList {
                it.CardID == CardId.ZAYLE_SHADOW_CLOAK
                        // We only set originalController for entities that start in a player's deck
                        // && it.extra.originalController == game.player!!.entity!!.PlayerID
                        && it.tags.get(Entity.KEY_ZONE) == Entity.ZONE_SETASIDE
            }.isNotEmpty()
        }
    }
}
