package net.mbonnin.arcanetracker

import kotlinx.coroutines.*
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.model.legacy.HSPlayer
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RGame
import net.mbonnin.arcanetracker.room.WLCounter
import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.GameType
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
                player_class = game.player!!.playerClass!!,
                opponent_class = game.opponent!!.playerClass!!,
                date = System.currentTimeMillis(),
                format_type = game.formatType.name,
                game_type = game.gameType.name,
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


    fun insertAndUploadGame(gameStr: ByteArray, gameStart: Date, currentOrFinishedGame: () -> Game?) {
        val summary = GameSummary()
        val game = currentOrFinishedGame()

        if (game == null) {
            return
        } else if (game.spectator) {
            return
        }

        Timber.d("ready to send hsreplay %s", game.gameType)

        if (game.gameType == GameType.GT_BATTLEGROUNDS) {
            uploadBattlegroundsGame(gameStr, gameStart)
            return
        }
        summary.coin = game.player!!.hasCoin
        summary.win = game.victory
        summary.hero = game.player!!.classIndex!!
        summary.opponentHero = game.opponent!!.classIndex!!
        summary.date = Utils.ISO8601DATEFORMAT.format(Date())
        summary.deckName = MainViewCompanion.playerCompanion.deck?.name

        GameSummary.addFirst(summary)

        val friendlyPlayer = game.player?.entity?.PlayerID ?: "1"


        val deck = MainViewCompanion.playerCompanion.deck?.let {
            it.cards.flatMap { entry ->
                Array(entry.value, { entry.key }).toList()
            }
        }

        val player = HSPlayer(
                rank = if (game.playerRank != RANK_UNKNOWN) game.playerRank else null,
                deck_id = MainViewCompanion.playerCompanion.deck?.id?.toLongOrNull(),
                deck = deck ?: emptyList()
        )

        val opponent = HSPlayer(
                rank = if (RankHolder.opponentRank != RANK_UNKNOWN) RankHolder.opponentRank else null,
                deck_id = null,
                deck = emptyList()
        )

        val player1 = if (friendlyPlayer == "1") player else opponent
        val player2 = if (friendlyPlayer == "1") opponent else player


        val uploadRequest = UploadRequest(
                match_start = Utils.ISO8601DATEFORMAT.format(gameStart),
                build = ArcaneTrackerApplication.get().hearthstoneBuild,
                spectator_mode = game.spectator,
                friendly_player = friendlyPlayer,
                format = game.formatType.intValue,
                game_type = fromGameAndFormat(game.gameType, game.formatType).intValue,
                player1 = player1,
                player2 = player2
        )

        GlobalScope.launch(Dispatchers.Main) {
            val insertResult = insertGame(game)

            if (ArcaneTrackerApplication.get().hsReplay.account() != null) {
                ArcaneTrackerApplication.get().analytics.logEvent("hsreplay_upload")
                val result = ArcaneTrackerApplication.get().hsReplay.uploadGame(uploadRequest, gameStr)
                when (result) {
                    is HsReplay.UploadResult.Success -> {
                        summary.hsreplayUrl = result.replayUrl
                        GameSummary.sync()

                        if (insertResult.success) {
                            withContext(Dispatchers.IO) {
                                RDatabaseSingleton.instance.gameDao().update(insertResult.id, result.replayUrl)
                            }
                        }

                        Timber.d("hsreplay upload success")
                        Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplaySuccess))
                    }
                    is HsReplay.UploadResult.Failure -> {
                        Timber.d(result.e)
                        Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplayError))
                    }
                }
            }
        }
    }

    private fun uploadBattlegroundsGame(gameStr: ByteArray, gameStart: Date) {
        val uploadRequest = UploadRequest(
                match_start = Utils.ISO8601DATEFORMAT.format(gameStart),
                build = ArcaneTrackerApplication.get().hearthstoneBuild,
                spectator_mode = false,
                format = FormatType.FT_WILD.intValue,
                game_type = BnetGameType.BGT_BATTLEGROUNDS.intValue
        )
        GlobalScope.launch(Dispatchers.Main) {
            try {
                ArcaneTrackerApplication.get().hsReplay.uploadGame(uploadRequest, gameStr)
            } catch (e: Exception) {
                // battlegrounds file are large and ktor
                Utils.reportNonFatal(e)
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

        ArcaneTrackerApplication.get().analytics.logEvent(
                "game_ended",
                mapOf(
                        EventParams.GAME_TYPE.value to game.gameType.name,
                        EventParams.FORMAT_TYPE.value to game.formatType.name,
                        EventParams.HSREPLAY.value to (ArcaneTrackerApplication.get().hsReplay.account() != null).toString()
                )
        )
    }


    private fun updateCounter(id: String, victory: Boolean) {
        val winsIncrement = if (victory) 1 else 0
        WLCounter.increment(id, winsIncrement, 1 - winsIncrement)
    }
}