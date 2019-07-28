package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.FormatType
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.GameType
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId
import net.hearthsim.hsmodel.enum.Mechanic
import net.hearthsim.hsmodel.enum.Rarity

class PossibleSecrets(val cardJson: CardJson) {
    data class AvailableSecretKey(val playerClass: String, val gameType: GameType, val formatType: FormatType)
    val cachedAvailableSecrets= mutableMapOf<AvailableSecretKey, List<String>>()

    fun getAll(game: Game): List<PossibleSecret> {
        val entities = game.getEntityList {
            it.tags[Entity.KEY_CONTROLLER] == game.opponentId()
            && it.tags[Entity.KEY_ZONE] == Entity.ZONE_SECRET
                    && Rarity.LEGENDARY != it.tags[Entity.KEY_RARITY]
        }

        val map = mutableMapOf<String, Int>()
        entities.forEach {entity ->
            availableSecretsCached(
                    playerClass = entity.tags[Entity.KEY_CLASS] ?: "",
                    formatType = game.formatType,
                    gameType = game.gameType
                    ).forEach {
                val possibleCount = map.getOrElse(it, {0})

                map.put(it, possibleCount + if (entity.extra.excludedSecretList.contains(it)) 0 else 1)
            }
        }

        return map.map {
            PossibleSecret(it.key, it.value)
        }
    }

    fun availableSecretsCached(playerClass: String, gameType: GameType, formatType: FormatType): List<String> {
        val key = AvailableSecretKey(playerClass, gameType, formatType)

        val cached = cachedAvailableSecrets.get(key)
        if (cached != null) {
            return cached
        }

        val list = availableSecrets(cardJson, playerClass, gameType, formatType)

        cachedAvailableSecrets.put(key, list)

        return list
    }

    companion object {
        fun availableSecrets(cardJson: CardJson, playerClass: String, gameType: GameType, formatType: FormatType): List<String> {
            var secrets = cardJson.allCards().filter {
                it.mechanics.contains(Mechanic.SECRET)
                        && it.playerClass == playerClass
                        && it.id != CardId.FLAME_WREATH  // these are bosses secrets
                        && it.id != CardId.FLAME_WREATH1
            }

            if (gameType == GameType.GT_ARENA) {
                secrets = secrets.filter {
                    Card.ARENA_SETS.contains(it.set)
                }
            } else if (formatType == FormatType.FT_STANDARD) {
                secrets = secrets.filter {
                    it.isStandard()
                }
            }

            return secrets.map { it.id }
        }
    }
}