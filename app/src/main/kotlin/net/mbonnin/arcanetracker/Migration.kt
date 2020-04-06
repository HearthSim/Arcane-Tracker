package net.mbonnin.arcanetracker

import kotlinx.coroutines.*
import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.GameType
import net.hearthsim.hslog.util.getPlayerClass
import net.mbonnin.arcanetracker.model.GameSummary
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.sqldelight.Game
import net.mbonnin.arcanetracker.sqldelight.mainDatabase
import timber.log.Timber

object Migration {
    private const val PAPERDB_ID = 2L
    private const val ROOM_ID = 3L

    fun migrateToSqlDelight() {
        val paperGames = GameSummary.gameSummaryList ?: emptyList()
        val roomGames = RDatabaseSingleton.instance.gameDao().selectAll().blockingFirst()

        val start = System.currentTimeMillis()
        val games1 = paperGames.map {
            Game.Impl(
                id = PAPERDB_ID,
                coin = if (it.coin) 1 else 0,
                match_start_millis = Utils.ISO8601DATEFORMAT.parse(it.date).time,
                deck_id = null,
                deck_name = it.deckName ?: "?",
                format_type = FormatType.FT_STANDARD.name, // PaperDb did not store this information, do a best guess here
                game_type = GameType.GT_RANKED.name, // PaperDb did not store this information, do a best guess here
                hs_replay_url = it.hsreplayUrl,
                opponent_player_class = getPlayerClass(it.opponentHero),
                player_player_class = getPlayerClass(it.hero),
                rank = null,
                victory = if (it.win) 1 else 0
            )
        }

        val games2 = roomGames.map {
            Game.Impl(
                id = ROOM_ID,
                coin = if (it.coin) 1 else 0,
                match_start_millis = it.date,
                deck_id = it.deck_id,
                deck_name = it.deck_name,
                format_type = it.format_type,
                game_type = it.game_type,
                hs_replay_url = it.hs_replay_url,
                opponent_player_class = it.opponent_class,
                player_player_class = it.player_class,
                rank = it.rank?.toLong(),
                victory = if (it.victory) 1 else 0
            )
        }

        val filtered = (games1 + games2).sortedBy { it.match_start_millis }
            .fold(mutableListOf<Game.Impl>()) { list, current ->
                if (list.isEmpty()) {
                    list.add(current)
                    return@fold list
                }

                val previous = list.last()


                if (previous.player_player_class == current.player_player_class
                    && previous.opponent_player_class == current.opponent_player_class
                    && previous.victory == current.victory
                    && previous.coin == current.coin
                    && ((current.match_start_millis ?: 0L) - (previous.match_start_millis ?: 0L)) < 5000L) {
                    // Remove duplicates that are both in Room and in PaperDb
                    if (previous.id == PAPERDB_ID){
                        // PaperDb did not store game_type and will have GT_VS_AI in place of the actual GameType
                        // So we remove it here and we'll add it again
                        list.removeAt(list.size - 1)
                        list.add(current)
                    }
                } else {
                    list.add(current)
                }
                list
            }


        Timber.d("SQLDelight intermediate: ${System.currentTimeMillis() - start}ms ")

        mainDatabase.transaction {
            filtered.forEach {
                mainDatabase.gameQueries.insertGame(
                    deck_id = it.deck_id,
                    victory = it.victory,
                    player_player_class = it.player_player_class,
                    opponent_player_class = it.opponent_player_class,
                    coin = it.coin,
                    match_start_millis = it.match_start_millis,
                    rank = it.rank,
                    hs_replay_url = it.hs_replay_url,
                    game_type = it.game_type,
                    format_type = it.format_type,
                    deck_name = it.deck_name
                )
            }
        }
        GameSummary.eraseGameSummary()
        runBlocking {
            // That's a bit ugly but we need this else Room complains about main thread access
            // The code above is blocking the mainthread anyways...
            withContext(Dispatchers.IO) {
                RDatabaseSingleton.instance.gameDao().deleteAll()
            }
        }

        val end = System.currentTimeMillis()

        Timber.d("Migrated to SQLDelight in ${end - start}ms (${filtered.size} items)")
    }
}