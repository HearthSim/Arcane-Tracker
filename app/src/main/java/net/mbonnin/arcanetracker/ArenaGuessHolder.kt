package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.detector.ArenaResult

object ArenaGuessHolder {
    class Filter {
        var cardId = ""
        var displayedCardId = ""
        var minDistance = Double.MAX_VALUE
    }

    private val filters = Array(3, { Filter() })

    @Synchronized
    fun setArena(arenaResult: Array<ArenaResult>, hero: String) {
        for (i in 0..2) {
            filters[i].minDistance = arenaResult[i].distance
            filters[i].cardId = arenaResult[i].cardId
            displayIfNeeded(i, filters[i], hero)
        }
    }

    private fun displayIfNeeded(index: Int, filter: Filter, hero: String) {
        if (filter.cardId != filter.displayedCardId) {
            Utils.runOnMainThread({ ArenaGuessCompanion.show(index, filter.cardId, hero) })
            filter.displayedCardId = filter.cardId
        }

    }


    @Synchronized
    fun clear() {
        for (i in 0..2) {
            if ("" != filters[i].cardId) {
                Utils.runOnMainThread { ArenaGuessCompanion.hide(i) }
                filters[i].cardId = ""
                filters[i].displayedCardId = ""
                filters[i].minDistance = Double.MAX_VALUE
            }
        }
    }
}