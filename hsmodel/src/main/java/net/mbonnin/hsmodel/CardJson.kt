package net.mbonnin.hsmodel

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.util.*
import com.squareup.moshi.ToJson
import com.squareup.moshi.FromJson


object CardJson {
    private val allCards = ArrayList<Card>()
    private val INVALID_PLAYER_CLASS = "INVALID_PLAYER_CLASS"
    private val INVALID_DFB_ID = Int.MIN_VALUE

    class CardAdapter(val lang: String) {
        @FromJson
        fun fromGson(hsCard: HSCard): Card {

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

        @ToJson
        fun toJson(card: Card): HSCard {
            return HSCard(id = card.id,
                    mechanics = card.mechanics.toList(),
                    name = emptyMap(),
                    attack = card.attack,
                    collectible = card.collectible,
                    cost = card.cost,
                    dbfId = card.dbfId,
                    durability = card.durability,
                    health = card.health,
                    multiClassGroup = card.multiClassGroup,
                    cardClass = card.playerClass,
                    race = card.race,
                    rarity = card.rarity,
                    set = card.set,
                    text = emptyMap(),
                    type = card.type
            )
        }
    }


    fun init(lang: String, injectedCards: List<Card>?) {
        val moshi = Moshi.Builder()
                .add(CardAdapter(lang))
                .build()

        val type = Types.newParameterizedType(List::class.java, Card::class.java)
        val adapter = moshi.adapter<List<Card>>(type)
        val bufferedSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream("cards.json")))
        val cardList = bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }

        val allCards = cardList
                ?.filter { it.dbfId != INVALID_DFB_ID } // removes "PlaceholderCard"
                ?.filter { it.playerClass != INVALID_PLAYER_CLASS } // removes a bunch of FB_LK_BossSetup cards
                ?.toMutableList() ?: mutableListOf()

        injectedCards?.let { allCards.addAll(it) }

        allCards.sortBy { it.id }
    }

    fun allCards(): List<Card> {
        return allCards
    }

    fun init(lang: String) {
        init(lang, null)
    }

    fun getCard(id: String): Card? {
        val index = Collections.binarySearch(allCards, id)
        return if (index < 0) {
            null
        } else {
            allCards[index]
        }
    }
}
