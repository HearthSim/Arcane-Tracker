package net.mbonnin.arcanetracker.model

import net.mbonnin.arcanetracker.PaperDb
import java.util.*

class GameSummary {
    var deckName: String? = null
    var hero: Int = 0
    var opponentHero: Int = 0
    var coin: Boolean = false
    var win: Boolean = false
    var date: String? = null

    var hsreplayUrl: String? = null
    // added in v213
    var bnetGameType: Int = 0

    companion object {

        internal val KEY_GAME_LIST = "KEY_GAME_LIST"
        internal var savedGameSummaryList: ArrayList<GameSummary>? = null

        init {
            savedGameSummaryList = PaperDb.read<ArrayList<GameSummary>?>(KEY_GAME_LIST)?.let { ArrayList(it) }
            if (savedGameSummaryList == null) {
                savedGameSummaryList = ArrayList()
            }
        }

        val gameSummaryList: List<GameSummary>?
            get() = savedGameSummaryList

        fun sync() {
            PaperDb.write<ArrayList<GameSummary>?>(KEY_GAME_LIST, savedGameSummaryList)
        }

        fun eraseGameSummary() {
            savedGameSummaryList!!.clear()
            sync()
        }
    }
}
