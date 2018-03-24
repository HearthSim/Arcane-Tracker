package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import net.mbonnin.hsmodel.Type
import okio.Okio
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KFunction2

class ByteBufferImage(val w: Int,
                      val h: Int,
                      val buffer: ByteBuffer /* RGBA buffer */,
                      val stride: Int/* in bytes */)

class MatchResult {
    var bestIndex = 0
    var distance = 0.0
}

class ArenaResult(var cardId: String = "", var distance: Double = 0.0)

const val INDEX_UNKNOWN = -1
const val RANK_UNKNOWN = INDEX_UNKNOWN

class Detector(var context: Context, val isTablet: Boolean) {
    private val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    private fun <T> decode(resourceName: String, type: java.lang.reflect.Type): T {
        val adapter = moshi.adapter<T>(type)
        val bufferedSource = Okio.buffer(Okio.source(CardJson::class.java.getResourceAsStream(resourceName)))
        return bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }!! // <= not really sure if moshi can return null values since it usually throws exceptions
    }

    var lastPlayerClass: String? = "?"
    val generatedData = decode<RankData>("/rank_data.json", RankData::class.java)
    val rectFactory = RRectFactory(isTablet)

    fun prepareImage(bbImage: ByteBufferImage) {
        rectFactory.prepareImage(bbImage)
    }

    fun detectPlayerRank(byteBufferImage: ByteBufferImage): Int {
        val matchResult = Detector.matchImage(byteBufferImage,
                rectFactory.playerRankRect(byteBufferImage),
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                generatedData.RANKS)

        if (matchResult.distance < 50.0) {
            return matchResult.bestIndex
        } else {
            return INDEX_UNKNOWN
        }
    }

    fun detectOpponentRank(byteBufferImage: ByteBufferImage): Int {
        val matchResult = Detector.matchImage(byteBufferImage,
                rectFactory.opponentRankRect(byteBufferImage),
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                generatedData.RANKS)

        if (matchResult.distance < 50.0) {
            return matchResult.bestIndex
        } else {
            return INDEX_UNKNOWN
        }
    }

    private lateinit var cardByType: Array<List<Card>>

    fun detectArena(byteBufferImage: ByteBufferImage, playerClass: String?): Array<ArenaResult> {
        val arenaResults = Array<ArenaResult>(3, { ArenaResult("", Double.MAX_VALUE) })

        if (playerClass != lastPlayerClass) {
            val subList = CardJson.allCards()
                    .filter { it.scores != null } // cards available in arena
                    .filter {
                        when {
                            playerClass == null -> true // running from tests
                            it.playerClass == PlayerClass.NEUTRAL -> true
                            it.playerClass == playerClass -> true
                            else -> false
                        }
                    }

            cardByType = arrayOf(
                    subList.filter { Type.MINION == it.type },
                    subList.filter { Type.SPELL == it.type },
                    subList.filter { Type.WEAPON == it.type }
            )
        }


        val arena_rects = arrayOf(rectFactory.arenaMinionRectArray(), rectFactory.arenaSpellRectArray(), rectFactory.arenaWeaponRectArray())

        for (position in 0 until 3) {
            var minDistance = Double.MAX_VALUE
            var bestId = ""
            for (type in 0 until 3) {
                val vector = extractHaar(byteBufferImage, arena_rects[type][position])
                for (card in cardByType[type]) {
                    var distance = manhattanDistance(vector, card.features!!)
                    if (distance < minDistance) {
                        minDistance = distance
                        bestId = card.id
                    }

                    distance = manhattanDistance(vector, card.goldenFeatures!!)
                    if (distance < minDistance) {
                        minDistance = distance
                        bestId = card.id
                    }
                }
            }

            arenaResults[position].distance = minDistance
            arenaResults[position].cardId = bestId
        }
        Log.d("Detector", arenaResults.map { "[" + it.cardId + "(" + it.distance + ")]" }.joinToString(","))
        return arenaResults
    }

    companion object {
        fun euclidianDistance(a: DoubleArray, b: DoubleArray): Double {
            var dist = 0.0
            for (i in 0 until a.size) {
                dist += (a[i] - b[i]) * (a[i] - b[i])
            }
            return dist
        }

        fun manhattanDistance(a: DoubleArray, b: DoubleArray): Double {
            var dist = 0.0
            for (i in 0 until a.size) {
                dist += Math.abs(a[i] - b[i])
            }
            return dist
        }

        fun extractHaar(byteBufferImage: ByteBufferImage, rrect: RRect): DoubleArray {
            return FeatureExtractor.INSTANCE.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, rrect)
        }

        fun <T> matchImage(byteBufferImage: ByteBufferImage, rrect: RRect, extractFeatures: KFunction2<ByteBufferImage, RRect, T>, computeDistance: KFunction2<T, T, Double>, candidates: List<T>, mapping: List<Int>? = null): MatchResult {

            val vector = extractFeatures(byteBufferImage, rrect)

            val matchResult = MatchResult()

            matchResult.bestIndex = INDEX_UNKNOWN
            matchResult.distance = Double.MAX_VALUE

            if (mapping != null) {
                for (mapIndex in mapping) {
                    val dist = computeDistance(vector, candidates[mapIndex])
                    //Log.d("Detector", String.format("%d: %f", rank, dist))
                    if (dist < matchResult.distance) {
                        matchResult.distance = dist
                        matchResult.bestIndex = mapIndex
                    }
                }
            } else {
                candidates.forEachIndexed { index, candidate ->
                    val dist = computeDistance(vector, candidate)
                    //Log.d("Detector", String.format("%d: %f", rank, dist))
                    if (dist < matchResult.distance) {
                        matchResult.distance = dist
                        matchResult.bestIndex = index
                    }
                }
            }
            return matchResult
        }

        fun haarToString(features: DoubleArray): String {
            val sb = StringBuilder()

            sb.append("=[")
            sb.append(features.map { String.format("%3.2f", it) }.joinToString(" "))
            sb.append("]")
            return sb.toString()
        }

        val NAME_TO_CARD_ID by lazy {
            val map = TreeMap<String, ArrayList<String>>()

            CardJson.allCards().filter { it.name != null }.forEach({
                val cardName = it.name!!
                        .toUpperCase()
                        .replace(" ", "_")
                        .replace(Regex("[^A-Z_]"), "")

                map.getOrPut(cardName, { ArrayList() }).add(it.id)
            })

            val nameToCardID = TreeMap<String, String>()

            for (entry in map) {
                entry.value.sort()
                for ((i, id) in entry.value.withIndex()) {
                    var name = entry.key
                    if (i > 0) {
                        name += i
                    }
                    nameToCardID.put(name, id)
                }
            }

            nameToCardID
        }
    }
}


