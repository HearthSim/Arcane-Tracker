package net.mbonnin.arcanetracker.detector

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

class TierScore(val Hero: String?, val Score: Int?)
class TierCard(val CardId: String?, val Hero: String?, val Scores: List<TierScore>)
class TierCards(val Cards: List<TierCard>)

class Tierlist(val context: Context) {

    val list by lazy {
        val inputStream = context.resources.openRawResource(R.raw.tierlist);
        val reader = InputStreamReader(inputStream)
        Gson().fromJson<TierCards>(reader, TierCards::class.java).Cards
    }

    companion object {

        private var INSTANCE: Tierlist? = null

        fun get(context: Context): Tierlist =
                INSTANCE ?: Tierlist(context).also { INSTANCE = it }
    }
}
