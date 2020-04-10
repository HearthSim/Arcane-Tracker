package net.hearthsim.hslog.util

import net.hearthsim.hslog.parser.power.*
import net.hearthsim.hsmodel.Card
import net.hearthsim.hsmodel.CardJson
import net.hearthsim.hsmodel.enum.CardId
import net.hearthsim.hsmodel.enum.Mechanic

class AvailableSecrets {
    /**
     * A small class to use as key when computing the available secrets
     */
    private data class AvailableSecretKey(val playerClass: String, val gameType: GameType, val formatType: FormatType)

    private val cachedAvailableSecrets = mutableMapOf<AvailableSecretKey, List<String>>()

    fun availableSecretsCached(cardJson: CardJson, playerClass: String, gameType: GameType, formatType: FormatType): List<String> {
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
        private fun availableSecrets(cardJson: CardJson, playerClass: String, gameType: GameType, formatType: FormatType): List<String> {
            var secrets = cardJson.allCards().filter {
                it.mechanics.contains(Mechanic.SECRET)
                    && it.playerClass == playerClass
                    && it.id != CardId.FLAME_WREATH  // these are bosses secrets
                    && it.id != CardId.FLAME_WREATH1
                    && it.id != CardId.BUBBLEHEARTH
            }

            if (gameType == GameType.GT_ARENA) {
                secrets = secrets.filter {
                    Card.ARENA_SETS.contains(it.set)
                }
            } else {
                // Hands of salvation is only in arena
                secrets = secrets.filterNot { it.id == CardId.HAND_OF_SALVATION }

                if (formatType == FormatType.FT_STANDARD) {
                    secrets = secrets.filter {
                        it.isStandard()
                    }
                }
            }

            return secrets.map { it.id }
        }
    }
}