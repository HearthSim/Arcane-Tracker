package net.mbonnin.hsmodel

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.lang.reflect.Type
import java.util.*


object CardJson {
    private val allCards = ArrayList<Card>()

    fun <T> decode(resourceName: String, type: Type): T {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val adapter = moshi.adapter<T>(type)
        val bufferedSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream(resourceName)))
        return bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }!! // <= not really sure if moshi can return null values since it usually throws exceptions
    }


    fun init(lang: String, injectedCards: List<Card>?) {
        val cardTranslation = decode<Map<String, CardTranslation>>("/card_translation_${lang}.json", Types.newParameterizedType(Map::class.java, String::class.java, CardTranslation::class.java))

        val cardData = decode<List<Card>>("/card_data.json", Types.newParameterizedType(List::class.java, Card::class.java))

        val augmentedCards = cardData
                .map {

                    it.copy(
                            name = cardTranslation[it.id]!!.name,
                            text = cardTranslation[it.id]!!.text,
                            features = null,
                            goldenFeatures = null
                    )
                }

        allCards.addAll(augmentedCards)

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
