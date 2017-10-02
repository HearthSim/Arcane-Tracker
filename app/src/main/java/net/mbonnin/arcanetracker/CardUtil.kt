package net.mbonnin.arcanetracker


import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import net.mbonnin.hsmodel.Type

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
        card.type = Type.SPELL
        card.text = Utils.getString(R.string.secretText)

        when (playerClass) {
            PlayerClass.PALADIN -> {
                card.id = "secret_p"
                card.cost = 1
                card.playerClass = PlayerClass.PALADIN
            }
            PlayerClass.HUNTER -> {
                card.id = "secret_h"
                card.cost = 2
                card.playerClass = PlayerClass.HUNTER
            }
            PlayerClass.MAGE -> {
                card.id = "secret_m"
                card.cost = 3
                card.playerClass = PlayerClass.MAGE
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
