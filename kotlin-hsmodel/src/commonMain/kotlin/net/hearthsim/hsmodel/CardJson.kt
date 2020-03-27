package net.hearthsim.hsmodel

import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import net.hearthsim.hsmodel.enum.HSSet
import okio.BufferedSource


class CardJson private constructor(cards: List<Card>) {
    private val allCards = mutableListOf<Card>()

    init {
        allCards.addAll(
                cards
                        .filter { it.dbfId != INVALID_DFB_ID } // removes "PlaceholderCard"
                        .filter { it.playerClass != INVALID_PLAYER_CLASS } // removes a bunch of FB_LK_BossSetup cards
        )

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
        fun fromMultiLangJson(lang: String, injectedCards: List<Card> = emptyList(), src: BufferedSource): CardJson {
            val str = src.readUtf8()
            val cards = Json.nonstrict.parse(HSCard.serializer().list, str).map { mapToCard(it, lang) }

            return CardJson(injectedCards + cards)
        }

        fun fromLocalizedJson(src: BufferedSource): CardJson {
            val str = src.readUtf8()
            val cards = Json.nonstrict.parse(LocalizedHSCard.serializer().list, str).map {
                mapToCard(it)
            }

            return CardJson(cards)
        }

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

        private fun mapToCard(hsCard: HSCard, lang: String): Card {
            return Card(id = hsCard.id,
                    mechanics = hsCard.mechanics?.toSet() ?: emptySet(),
                    name = hsCard.name?.get(lang) ?: "",
                    attack = hsCard.attack,
                    collectible = hsCard.collectible ?: false,
                    cost = hsCard.cost,
                    dbfId = hsCard.dbfId ?: INVALID_DFB_ID,
                    durability = hsCard.durability,
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

        private fun mapToCard(hsCard: LocalizedHSCard): Card {
            return Card(id = hsCard.id,
                    mechanics = hsCard.mechanics?.toSet() ?: emptySet(),
                    name = hsCard.name ?: "",
                    attack = hsCard.attack,
                    collectible = hsCard.collectible ?: false,
                    cost = hsCard.cost,
                    dbfId = hsCard.dbfId ?: INVALID_DFB_ID,
                    durability = hsCard.durability,
                    health = hsCard.health,
                    multiClassGroup = hsCard.multiClassGroup,
                    playerClass = hsCard.cardClass ?: INVALID_PLAYER_CLASS,
                    race = hsCard.race,
                    rarity = hsCard.rarity,
                    set = hsCard.set ?: "CORE",
                    text = hsCard.text ?: "",
                    type = hsCard.type ?: ""
            )
        }

        private val INVALID_PLAYER_CLASS = "INVALID_PLAYER_CLASS"
        private val INVALID_DFB_ID = Int.MIN_VALUE
    }
}
