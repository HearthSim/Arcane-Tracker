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
        val tierlist = decode<TierCards>("/tierlist.json", TierCards::class.java)
        val arenaData = decode<ArenaData>("/arena_data.json", ArenaData::class.java)

        val tierCards = tierlist.Cards.sortedBy { it.CardId }

        val augmentedCards = cardData
                .map {

                    val card = it
                    var index = tierCards.binarySearch { it.CardId.compareTo(card.id) }
                    var tierCard = if (index > 0) tierCards[index] else null

                    index = arenaData.ids.binarySearch { it.compareTo(card.id) }
                    var (features, goldenFeatures) = if (index > 0)
                        arenaData.features[index] to arenaData.featuresGolden[index]
                    else
                        null to null

                    if (!it.isDraftable()) {
                        tierCard = null
                        features = null
                        goldenFeatures = null
                    }

                    it.copy(
                            name = cardTranslation[it.id]!!.name,
                            text = cardTranslation[it.id]!!.text,
                            scores = tierCard?.Scores,
                            features = features,
                            goldenFeatures = goldenFeatures
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
