package net.mbonnin.hsmodel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collections

/**
 * Created by martin on 9/25/17.
 */

object CardJson {
    private var list: ArrayList<Card>? = null

    fun init(lang: String, injectedCards: List<Card>?) {
        val inputStream = CardJson::class.java.getResourceAsStream("/cards_$lang.json")
        val reader = InputStreamReader(inputStream)
        list = Gson().fromJson<ArrayList<Card>>(reader, object : TypeToken<ArrayList<Card>>() {

        }.type)

        injectedCards?.let { list!!.addAll(injectedCards) }

        Collections.sort(list!!) { a, b -> a.id?.compareTo(b?.id ?: "") ?: 0 }
    }

    fun allCards(): List<Card> {
        return list!!
    }
}
