package net.mbonnin.arcanetracker


import net.mbonnin.hsmodel.*

object CardUtil {
    @JvmField
    val UNKNOWN = unknown()

    fun unknown(name: String? = null): Card {
        val card = Card(
                id = "?",
                name = name ?: "?",
                playerClass = "?",
                cost = Card.UNKNOWN_COST,
                rarity = "?",
                type = Card.UNKNOWN_TYPE,
                text = "?",
                race = "?",
                collectible = false,
                dbfId = 0,
                set = HSSet.CORE
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
            PlayerClass.ROGUE -> {
                id = "secret_r"
                cost = 2
                pClass = PlayerClass.ROGUE
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
                playerClass = pClass,
                dbfId = 0,
                set = HSSet.CORE
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

    fun possibleSecretList(playerClass: String?, gameType: String?, formatType: String?): Collection<String> {

        var secrets = CardJson.allCards().filter {
            it.mechanics.contains(Mechanic.SECRET)
                    && it.playerClass == playerClass
                    && (formatType != FormatType.FT_STANDARD.name || it.isStandard())
        }

        if (gameType == GameType.GT_ARENA.name) {
            secrets = secrets.filter {
                it.isStandard()
            }
        } else if (formatType == FormatType.FT_STANDARD.name) {
            secrets = secrets.filter {
                it.isStandard()
            }
        }

        return secrets.map { it.id }
    }

    fun getDust(rarity: String?, golden: Boolean): Int {
        if (rarity == null) {
            return 0
        }
        return when (rarity) {
            Rarity.COMMON -> if (golden) 50 else 5
            Rarity.RARE -> if (golden) 100 else 20
            Rarity.EPIC -> if (golden) 400 else 100
            Rarity.LEGENDARY -> if (golden) 1600 else 400
            else -> 0
        }
    }
}
