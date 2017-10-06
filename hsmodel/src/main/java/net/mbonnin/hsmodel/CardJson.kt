package net.mbonnin.hsmodel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.*


object CardJson {
    private lateinit var allCards: ArrayList<Card>

    fun init(lang: String, injectedCards: List<Card>?) {
        val reader = InputStreamReader(CardJson::class.java.getResourceAsStream("/cards_$lang.json"))
        allCards = Gson().fromJson<ArrayList<Card>>(reader, object : TypeToken<ArrayList<Card>>() {}.type)

        injectedCards?.let { allCards.addAll(it) }

        Collections.sort(allCards) { a, b -> a.id.compareTo(b.id) }

        val tierListReader = InputStreamReader(CardJson::class.java.getResourceAsStream("/tierlist.json"))
        val tiercards = Gson().fromJson<TierCards>(tierListReader, TierCards::class.java).Cards
                .sortedBy { it.CardId }

        for (tiercard in tiercards) {
            val card = getCard(tiercard.CardId)
            card!!.scores = tiercard.Scores
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
