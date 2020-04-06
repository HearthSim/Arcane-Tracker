package net.mbonnin.arcanetracker

import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.GameType
import net.hearthsim.hsreplay.HsReplay
import net.hearthsim.hsreplay.model.legacy.HSPlayer
import net.hearthsim.hsreplay.model.legacy.UploadRequest
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RGame
import net.mbonnin.arcanetracker.room.WLCounter
import net.mbonnin.arcanetracker.sqldelight.mainDatabase
import net.mbonnin.arcanetracker.ui.overlay.view.MainViewCompanion
import timber.log.Timber
import java.util.*

object GameHelper {

    sealed class InsertResult {
        class Success(val id: Long) : InsertResult()
        object Error : InsertResult()
    }

    fun (Boolean?).toLong() = if (this == true) 1L else 0L

    private suspend fun insertGame(game: Game, gameStart: Date): InsertResult {
        val deck = MainViewCompanion.playerCompanion.deck
        if (deck == null) {
            return InsertResult.Error
        }

        return withContext(Dispatchers.IO) {
            mainDatabase.gameQueries.insertGame(
                deck_id = deck.id,
                match_start_millis = gameStart.time,
                deck_name = deck.name!!,
                format_type = game.formatType.name,
                game_type = game.gameType.name,
                rank = game.playerRank.toLong(),
                hs_replay_url = null,
                coin = game.player!!.hasCoin.toLong(),
                opponent_player_class = game.opponent!!.playerClass!!,
                player_player_class = game.player!!.playerClass!!,
                victory = game.victory.toLong()
            )
            val id = mainDatabase.gameQueries.lastRowId().executeAsOne()
            InsertResult.Success(id)
        }
    }

    private fun createUploadRequest(game: Game, gameStart: Date, deck: Deck?, hearthstoneBuild: Int): UploadRequest {
        val friendlyPlayer = game.player?.entity?.PlayerID ?: "1"

        val cards = deck?.let {
            it.cards.flatMap { entry ->
                Array(entry.value, { entry.key }).toList()
            }
        }

        val player = HSPlayer(
            player_id = game.player?.entity?.PlayerID?.toInt() ?: -1,
            deck_id = deck?.id?.toLongOrNull(),
            deck = cards ?: emptyList(),
            star_level = null
        )

        val opponent = HSPlayer(
            player_id = game.opponent?.entity?.PlayerID?.toInt() ?: -1,
            deck_id = null,
            deck = emptyList(),
            star_level = null
        )

        return UploadRequest(
            match_start = Utils.ISO8601DATEFORMAT.format(gameStart),
            build = hearthstoneBuild,
            spectator_mode = game.spectator,
            friendly_player = friendlyPlayer,
            format = game.formatType.intValue,
            game_type = fromGameAndFormat(game.gameType, game.formatType).intValue,
            players = listOf(player, opponent),
            league_id = 5
        )
    }

    fun insertAndUploadGame(gameStr: ByteArray, gameStart: Date, currentOrFinishedGame: () -> Game?) {
        val game = currentOrFinishedGame()

        if (game == null) {
            return
        } else if (game.spectator) {
            return
        }

        Timber.d("ready to send hsreplay %s", game.gameType)

        val deck = when (game.gameType) {
            GameType.GT_BATTLEGROUNDS,
            GameType.GT_VS_AI,
            GameType.GT_TAVERNBRAWL -> null
            else -> MainViewCompanion.playerCompanion.deck
        }
        val uploadRequest = createUploadRequest(game,
            gameStart,
            deck,
            ArcaneTrackerApplication.get().hearthstoneBuild
        )

        GlobalScope.launch(Dispatchers.Main) {
            val insertResult = insertGame(game, gameStart)

            if (ArcaneTrackerApplication.get().hsReplay.account() == null) {
                return@launch
            }
            ArcaneTrackerApplication.get().analytics.logEvent("hsreplay_upload")
            val result = ArcaneTrackerApplication.get().hsReplay.uploadGame(uploadRequest, gameStr)
            when (result) {
                is HsReplay.UploadResult.Success -> {
                    if (insertResult is InsertResult.Success) {
                        withContext(Dispatchers.IO) {
                            mainDatabase.gameQueries.setReplayUrl(id = insertResult.id, replay_url = result.replayUrl)
                        }
                    }

                    Timber.d("hsreplay upload success")
                    when (game.gameType) {
                        GameType.GT_CASUAL,
                        GameType.GT_ARENA,
                        GameType.GT_RANKED -> {
                            val toast = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.toast_hsreplay_upload, null, false)

                            toast.setOnClickListener {
                                Utils.openLink(result.replayUrl)
                            }
                            Toaster.show(toast)
                        }
                    }
                }
                is HsReplay.UploadResult.Failure -> {
                    Timber.d(result.e)
                    //Toaster.show(ArcaneTrackerApplication.context.getString(R.string.hsreplayError))
                }
            }
        }
    }

    fun gameEnded(game: Game) {
        game.playerRank = RankHolder.playerRank
        game.opponentRank = RankHolder.opponentRank

        RankHolder.reset()

        Timber.w("gameOver  %s [gameType %s][format_type %s]",
            if (game.victory == true) "victory" else "lost",
            game.gameType,
            game.formatType)

        MainViewCompanion.playerCompanion.deck?.let { updateCounter(it.id!!, game.victory == true) }

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