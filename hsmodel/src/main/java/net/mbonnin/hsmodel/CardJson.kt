package net.mbonnin.hsmodel

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.lang.reflect.Type
import java.util.*




object CardJson {
    private val allCards = ArrayList<Card>()
    private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    private fun <T> decode(resourceName: String, type: Type): T {
        val adapter = moshi.adapter<T>(type)
        val bufferedSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream(resourceName)))
        return bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }!! // <= not really sure if moshi can return null values since it usually throws exceptions
    }

    val cardData = decode<List<Card>>("/card_data.json", Types.newParameterizedType(List::class.java, Card::class.java))
    val tierlist = decode<TierCards>("/tierlist.json", TierCards::class.java)

    fun init(lang: String, injectedCards: List<Card>?) {
        val cardTranslation = decode<Map<String, CardTranslation>>("/card_translation_${lang}.json", Types.newParameterizedType(Map::class.java, String::class.java, CardTranslation::class.java))

        var tierCards = tierlist.Cards.sortedBy { it.CardId }

        allCards.addAll(cardData
                .map {

                    val cardId = it
                    val index = tierCards.binarySearch { cardId.compareTo(it.CardId) }
                    val tierCard = if (index > 0) tierCards[index] else null

                    Card(id = it.id,
                            name = cardTranslation[it.id]!!.name,
                            text = cardTranslation[it.id]!!.text,
                            playerClass = it.playerClass,
                            rarity =  it.rarity,
                            race = it.race,
                            type = it.type,
                            set = it.set,
                            dbfId = it.dbfId,
                            cost = it.cost,
                            attack = it.attack,
                            health = it.health,
                            durability = it.durability,
                            collectible = it.collectible,
                            multiClassGroup = it.multiClassGroup,
                            scores = tierCard?.Scores)
                })

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
