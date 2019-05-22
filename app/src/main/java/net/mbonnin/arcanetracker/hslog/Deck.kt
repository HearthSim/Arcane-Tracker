package net.mbonnin.arcanetracker.hslog

import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.helper.getClassIndex
import net.mbonnin.arcanetracker.helper.getPlayerClass
import net.mbonnin.hsmodel.Card
import timber.log.Timber

class Deck {

    var cards = mutableMapOf<String, Int>()
    var name: String? = null
    var classIndex: Int = 0
    var id: String? = null
    var wins: Int = 0
    var losses: Int = 0

    fun checkClassIndex() {
        for (cardId in cards.keys) {
            val (_, _, _, playerClass) = CardUtil.getCard(cardId)
            val ci = getClassIndex(playerClass)
            if (ci >= 0 && ci < Card.CLASS_INDEX_NEUTRAL) {
                if (classIndex != ci) {
                    Timber.e("inconsistent class index, force to" + getPlayerClass(ci))
                    classIndex = ci
                }
                return
            }
        }
    }

    companion object {
        const val MAX_CARDS = 30
    }
}
