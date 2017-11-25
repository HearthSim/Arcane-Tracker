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
                // Add any other JsonAdapter factories.
                .add(KotlinJsonAdapterFactory())
                .build()

        val listMyData = Types.newParameterizedType(List::class.java, Card::class.java)

        val parsedCards = moshi.adapter<List<Card>>(listMyData).fromJson(Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream("/cards_$lang.json"))))

        // this will crash if something wrong happens during deserialisation but that's what we want
        allCards.addAll(parsedCards!!)

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
