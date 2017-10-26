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
        if (System.currentTimeMillis() - clearTime < 1000) {
            return
        }
        for (i in 0..2) {
            if (arenaResult[i].distance < filters[i].minDistance) {
                filters[i].minDistance = arenaResult[i].distance
                filters[i].cardId = arenaResult[i].cardId
                displayIfNeeded(i, filters[i], hero)
            }
        }
    }

    private fun displayIfNeeded(index: Int, filter: Filter, hero: String) {
        if (filter.cardId != filter.displayedCardId) {
            Utils.runOnMainThread({ ArenaGuessCompanion.show(index, filter.cardId, hero) })
            filter.displayedCardId = filter.cardId
        }

    }

    private var clearTime: Long = 0

    @Synchronized
    fun clear() {
        clearTime = System.currentTimeMillis()
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