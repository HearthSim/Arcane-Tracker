package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.Log
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import net.mbonnin.hsmodel.Type
import okio.Okio
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

const val FORMAT_UNKNOWN = INDEX_UNKNOWN
const val FORMAT_WILD = 0
const val FORMAT_STANDARD = 1

const val MODE_UNKNOWN = INDEX_UNKNOWN
private const val MODE_CASUAL_STANDARD = 0
private const val MODE_CASUAL_WILD = 1
private const val MODE_RANKED_STANDARD = 2
private const val MODE_RANKED_WILD = 3
const val MODE_CASUAL = 4
const val MODE_RANKED = 5

class Detector(var context: Context) {
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

    private val arena_rects by lazy {
        arrayOf(rectFactory!!.ARENA_MINIONS, rectFactory!!.ARENA_SPELLS, rectFactory!!.ARENA_WEAPONS)
    }

    var lastPlayerClass: String? = "?"
    val generatedData = decode<FormatModeRankData>("/format_mode_rank_data.json", FormatModeRankData::class.java)
    var rectFactory: RRectFactory? = null
    val rankMinimum = OneSecondMinimum(threshold = 50.0, tag = "rank")
    // mode uses the glowing blue area that is subject to a bit more variation
    val modeMinimum = OneSecondMinimum(threshold = 50.0, tag = "mode")
    val formatMinimum = OneSecondMinimum(threshold = 50.0, tag = "format")

    private fun ensureRectFactory(byteBufferImage: ByteBufferImage) {
        if (rectFactory == null) {
            rectFactory = RRectFactory(byteBufferImage.w, byteBufferImage.h, context)
        }
    }

    fun detectRank(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        return rankMinimum.detect(byteBufferImage,
                rectFactory!!.RANK,
                generatedData.RANKS)
    }

    fun detectFormat(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        return formatMinimum.detect(byteBufferImage,
                rectFactory!!.FORMAT,
                generatedData.FORMATS)
    }

    fun detectMode(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        val index = modeMinimum.detect(byteBufferImage,
                rectFactory!!.MODE,
                if (rectFactory!!.isTablet) generatedData.MODES_TABLET else generatedData.MODES)

        when (index) {
            MODE_CASUAL_STANDARD, MODE_CASUAL_WILD -> return MODE_CASUAL
            MODE_RANKED_STANDARD, MODE_RANKED_WILD -> return MODE_RANKED
            else -> return MODE_UNKNOWN
        }
    }

    private lateinit var cardByType: Array<List<Card>>

    fun detectArena(byteBufferImage: ByteBufferImage, playerClass: String?): Array<ArenaResult> {
        ensureRectFactory(byteBufferImage)
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

        fun formatString(format: Int): String {
            return when (format) {
                FORMAT_WILD -> "WILD"
                FORMAT_STANDARD -> "STANDARD"
                else -> "UNKNOWN"
            }
        }

        fun formatMode(mode: Int): String {
            return when (mode) {
                MODE_CASUAL -> "MODE_CASUAL"
                MODE_RANKED -> "MODE_RANKED"
                else -> "UNKNOWN"
            }
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


