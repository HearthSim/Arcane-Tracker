package net.mbonnin.arcanetracker


import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.GameType
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.*

object CardUtil {
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
            else -> return CardJson.unknown()
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


    fun getCard(key: String): Card {
        return ArcaneTrackerApplication.get().cardJson.getCard(key)
    }

    fun possibleSecretList(playerClass: String?, gameType: String?, formatType: String?): Collection<String> {

        var secrets = ArcaneTrackerApplication.get().cardJson.allCards().filter {
            it.mechanics.contains(Mechanic.SECRET)
                    && it.playerClass == playerClass
                    && (formatType != FormatType.FT_STANDARD.name || it.isStandard())
                    && it.id != CardId.FLAME_WREATH  // these are bosses secrets
                    && it.id != CardId.FLAME_WREATH1
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
