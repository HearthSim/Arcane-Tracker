package net.mbonnin.arcanetracker


import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import net.mbonnin.hsmodel.Type

object CardUtil {
    @JvmField
    val UNKNOWN = unknown()

    fun unknown(name: String? = null): Card {
        val card = Card(
                id = "?",
                name = name?:"?",
                playerClass = "?",
                cost = Card.UNKNOWN_COST,
                rarity = "?",
                type = Card.UNKNOWN_TYPE,
                text = "?",
                race = "?",
                collectible = false
        )
        return card
    }

    fun secret(playerClass: String): Card {
        val id: String
        val cost: Int
        val pClass: String

        when (playerClass) {
            PlayerClass.PALADIN -> {
                id = "secret_p"
                cost = 1
                pClass = PlayerClass.PALADIN
            }
            PlayerClass.HUNTER -> {
                id = "secret_h"
                cost = 2
                pClass = PlayerClass.HUNTER
            }
            PlayerClass.MAGE -> {
                id = "secret_m"
                cost = 3
                pClass = PlayerClass.MAGE
            }
            else -> return unknown()
        }

        return Card(
                type = Type.SPELL,
                text = Utils.getString(R.string.secretText),
                name = Utils.getString(R.string.secret),
                id = id,
                cost = cost,
                playerClass = pClass
        )
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
