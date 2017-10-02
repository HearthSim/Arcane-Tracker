package net.mbonnin.hsmodel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.*


object CardJson {
    private lateinit var list: ArrayList<Card>

    fun init(lang: String, injectedCards: List<Card>?) {
        val inputStream = CardJson::class.java.getResourceAsStream("/cards_$lang.json")
        val reader = InputStreamReader(inputStream)
        list = Gson().fromJson<ArrayList<Card>>(reader, object : TypeToken<ArrayList<Card>>() {}.type)

        injectedCards?.let { list.addAll(it) }

        Collections.sort(list) { a, b -> a.id?.compareTo(b?.id ?: "") ?: 0 }
    }

    fun allCards(): List<Card> {
        return list
    }

    fun init(lang: String) {
        init(lang, null)
    }

    fun getCard(id: String): Card? {
        val index = Collections.binarySearch(list, id)
        return if (index < 0) {
            null
        } else {
            list[index]
        }
    }
}
