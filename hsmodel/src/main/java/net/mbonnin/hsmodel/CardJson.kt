package net.mbonnin.hsmodel

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.util.*




object CardJson {
    private val allCards = ArrayList<Card>()

    fun init(lang: String, injectedCards: List<Card>?) {
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val cardDataType = Types.newParameterizedType(Map::class.java, String::class.java, Card::class.java)
        val cardDataSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream("/cardData.json")))
        val cardData = cardDataSource.use {
            moshi.adapter<List<Card>>(cardDataType).fromJson(cardDataSource)
        }

        val cardTranslationType = Types.newParameterizedType(Map::class.java, String::class.java, Map::class.java)
        val cardTranslationSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream("/cardTranslation.json")))
        val cardTranslation = cardTranslationSource.use {
            moshi.adapter<Map<String, Map<String, CardTranslation>>>(cardTranslationType).fromJson(cardTranslationSource)!!
        }

        // this will crash if something wrong happens during deserialisation but that's what we want
        allCards.addAll(cardData!!
                .map {
                    val cardTranslationLang = cardTranslation[lang]!!
                    Card(id = it.id,
                            name = cardTranslationLang[it.id]!!.name,
                            text = cardTranslationLang[it.id]!!.text,
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
                            collectible = it.collectible)
                })

        injectedCards?.let { allCards.addAll(it) }

        Collections.sort(allCards) { a, b -> a.id.compareTo(b.id) }


        val bufferedSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream("/tierlist.json")))
        val tiercards = moshi.adapter(TierCards::class.java).fromJson(bufferedSource)

        // this will crash if something wrong happens during deserialisation but that's what we want
        tiercards!!.Cards
                .sortedBy {  it.CardId }
                .forEach {
                    val card = getCard(it.CardId)
                    card!!.scores = it.Scores
                }
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
