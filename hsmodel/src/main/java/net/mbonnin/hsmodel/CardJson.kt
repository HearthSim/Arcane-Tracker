package net.mbonnin.hsmodel

import kotlinx.io.core.Input
import kotlinx.io.core.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import net.mbonnin.hsmodel.enum.HSSet


class CardJson(lang: String, injectedCards: List<Card>? = null, input: Input) {
    private val allCards = mutableListOf<Card>()
    private val INVALID_PLAYER_CLASS = "INVALID_PLAYER_CLASS"
    private val INVALID_DFB_ID = Int.MIN_VALUE


    private fun mapToCard(hsCard: HSCard, lang: String): Card {
        return Card(id = hsCard.id,
                mechanics = hsCard.mechanics?.toSet() ?: emptySet(),
                name = hsCard.name?.get(lang) ?: "",
                attack = hsCard.attack,
                collectible = hsCard.collectible ?: false,
                cost = hsCard.cost,
                dbfId = hsCard.dbfId ?: INVALID_DFB_ID,
                durability = hsCard.durability,
                features = null,
                goldenFeatures = null,
                health = hsCard.health,
                multiClassGroup = hsCard.multiClassGroup,
                playerClass = hsCard.cardClass ?: INVALID_PLAYER_CLASS,
                race = hsCard.race,
                rarity = hsCard.rarity,
                set = hsCard.set ?: "CORE",
                text = hsCard.text?.get(lang) ?: "",
                type = hsCard.type ?: ""
        )
    }



    init {
        val str = input.readText()

        val cardList = Json.nonstrict.parse(HSCard.serializer().list, str).map { mapToCard(it, lang) }

        allCards.addAll(
                cardList
                        .filter { it.dbfId != INVALID_DFB_ID } // removes "PlaceholderCard"
                        .filter { it.playerClass != INVALID_PLAYER_CLASS } // removes a bunch of FB_LK_BossSetup cards
        )

        injectedCards?.let { allCards.addAll(it) }

        allCards.sortBy { it.id }
    }

    fun allCards(): List<Card> {
        return allCards
    }

    fun getCard(id: String): Card {
        val index = allCards.binarySearch {
            it.id.compareTo(id)
        }
        return if (index < 0) {
            UNKNOWN
        } else {
            allCards[index]
        }
    }

    fun getCard(dbfId: Int): Card? {

        for (card in allCards) {
            if (card.dbfId == dbfId) {
                return card
            }
        }

        return null
    }

    companion object {
        val UNKNOWN = unknown()

        fun unknown(name: String? = null): Card {
            return Card(
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
        }
    }
}
