package net.mbonnin.arcanetracker


import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.playerclass.HUNTER
import net.mbonnin.hsmodel.playerclass.MAGE
import net.mbonnin.hsmodel.playerclass.PALADIN
import net.mbonnin.hsmodel.type.SPELL

object CardUtil {
    @JvmField
    val UNKNOWN = unknown()

    fun unknown(): Card {
        val card = Card()
        card.name = "?"
        card.playerClass = "?"
        card.cost = Card.UNKNOWN_COST
        card.id = "?"
        card.rarity = "?"
        card.type = Card.UNKNOWN_TYPE
        card.text = "?"
        card.race = "?"
        card.collectible = false
        return card
    }

    fun secret(playerClass: String): Card {
        val card = unknown()
        card.type = SPELL
        card.text = Utils.getString(R.string.secretText)

        when (playerClass) {
            PALADIN -> {
                card.id = "secret_p"
                card.cost = 1
                card.playerClass = PALADIN
            }
            HUNTER -> {
                card.id = "secret_h"
                card.cost = 2
                card.playerClass = HUNTER
            }
            MAGE -> {
                card.id = "secret_m"
                card.cost = 3
                card.playerClass = MAGE
            }
        }
        card.name = Utils.getString(R.string.secret)
        return card
    }

    fun getCard(dbfId: Int): Card? {

        for (card in CardJson.allCards()) {
            if (card.dbfId == dbfId) {
                return card
            }
        }

        return null
    }

    fun getCard(key: String): Card {
        return CardJson.getCard(key) ?: UNKNOWN
    }
}
