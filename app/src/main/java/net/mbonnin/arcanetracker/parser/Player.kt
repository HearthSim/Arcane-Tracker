package net.mbonnin.arcanetracker.parser

import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.getClassIndex

/**
 * Created by martin on 11/7/16.
 */
class Player {
    var entity: Entity? = null
    var battleTag: String? = null
    var isOpponent: Boolean = false
    var hasCoin: Boolean = false

    var hero: Entity? = null
    var heroPower: Entity? = null

    fun classIndex(): Int {
        val (_, _, _, playerClass) = CardUtil.getCard(hero!!.CardID!!)
        return getClassIndex(playerClass)
    }


    fun playerClass(): String {
        val (_, _, _, playerClass) = CardUtil.getCard(hero!!.CardID!!)
        return playerClass
    }
}
