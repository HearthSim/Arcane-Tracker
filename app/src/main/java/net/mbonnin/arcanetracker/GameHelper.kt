package net.mbonnin.arcanetracker

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import net.mbonnin.arcanetracker.hslog.Game
import net.mbonnin.arcanetracker.hslog.HSLog
import net.mbonnin.arcanetracker.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RGame
import net.mbonnin.arcanetracker.room.WLCounter
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import timber.log.Timber
import java.util.*

object GameHelper {
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
                deck_name = deck.name!!
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

    fun insertAndUploadGame(gameStr: String, gameStart: Date, hsLog: HSLog) {
        val summary = GameSummary()
        val game = hsLog.currentOrFinishedGame()

        if (game == null) {
            return
        } else if (game.spectator) {
            return
        }

        Timber.d("ready to send hsreplay %s", game.gameType)

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

    fun gameEnded(game: Game) {
        game.playerRank = RankHolder.playerRank
        game.opponentRank = RankHolder.opponentRank

        RankHolder.reset()

        Timber.w("gameOver  %s [gameType %s][format_type %s]",
                if (game.victory) "victory" else "lost",
                game.gameType,
                game.formatType)

        MainViewCompanion.playerCompanion.deck?.let { updateCounter(it.id!!, game.victory) }

        FileTree.get().sync()

        val bundle = Bundle()
        bundle.putString(EventParams.GAME_TYPE.value, game.gameType)
        bundle.putString(EventParams.FORMAT_TYPE.value, game.formatType)
        bundle.putString(EventParams.HSREPLAY.value, (ArcaneTrackerApplication.get().hsReplay.token() != null).toString())
        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.context).logEvent("game_ended", bundle)
    }


    private fun updateCounter(id: String, victory: Boolean) {
        val winsIncrement = if (victory) 1 else 0
        WLCounter.increment(id, winsIncrement, 1 - winsIncrement)
    }
}