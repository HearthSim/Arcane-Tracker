package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import net.mbonnin.hsmodel.CardJson
import net.mbonnin.hsmodel.PlayerClass
import java.io.InputStreamReader
import java.nio.ByteBuffer
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
    var mapping: List<Int> = listOf()
    var lastHero: String = ""
    val generatedData = Gson().fromJson(InputStreamReader(context.resources.openRawResource(R.raw.generated_data)), GeneratedData::class.java)
    var rectFactory: RRectFactory? = null
    var forceIsTablet = false
    var isTablet: Boolean = false
        set(value) {
            rectFactory?.isTablet = value
            forceIsTablet = true
            field = value
        }

    private fun ensureRectFactory(byteBufferImage: ByteBufferImage) {
        if (rectFactory == null) {
            rectFactory = RRectFactory(byteBufferImage.w, byteBufferImage.h, context)
            if (forceIsTablet) {
                rectFactory?.isTablet = isTablet
            }
        }
    }

    fun detectRank(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        val matchResult = matchImage(byteBufferImage,
                rectFactory!!.RANK,
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                generatedData.RANKS)

        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "rank: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectFormat(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        val matchResult = matchImage(byteBufferImage,
                rectFactory!!.FORMAT,
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                generatedData.FORMATS)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectMode(byteBufferImage: ByteBufferImage): Int {
        ensureRectFactory(byteBufferImage)
        val matchResult = matchImage(byteBufferImage,
                rectFactory!!.MODE,
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                if (rectFactory!!.isTablet) generatedData.MODES_TABLET else generatedData.MODES)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "mode: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        when (matchResult.bestIndex) {
            MODE_CASUAL_STANDARD, MODE_CASUAL_WILD -> return MODE_CASUAL
            MODE_RANKED_STANDARD, MODE_RANKED_WILD -> return MODE_RANKED
            else -> return MODE_UNKNOWN
        }
    }

    fun detectArenaHaar(byteBufferImage: ByteBufferImage, hero: String): Array<ArenaResult> {
        ensureRectFactory(byteBufferImage)
        return detectArena(byteBufferImage, Detector.Companion::extractHaar, Detector.Companion::euclidianDistance, generatedData.TIERLIST_HAAR, hero)
    }

    fun detectArenaPhash(byteBufferImage: ByteBufferImage, hero: String): Array<ArenaResult> {
        ensureRectFactory(byteBufferImage)
        return detectArena(byteBufferImage, Detector.Companion::extractPhash, Detector.Companion::hammingDistance, generatedData.TIERLIST_PHASH, hero)
    }

    private fun <T> detectArena(byteBufferImage: ByteBufferImage, extractFeatures: KFunction2<ByteBufferImage, RRect, T>, computeDistance: KFunction2<T, T, Double>, candidates: List<T>, hero: String): Array<ArenaResult> {
        val arenaResults = Array<ArenaResult?>(3,{null})

        if (hero != lastHero) {
            mapping = CardJson.allCards().filter { it.scores != null }.mapIndexed { index, card ->
                if (!PlayerClass.NEUTRAL.equals(card.playerClass) && hero != card.playerClass) {
                    // do not consider cards that are the wrong hero
                    null
                } else {
                    index
                }
            }.filterNotNull()

            lastHero = hero
        }
        val sb = StringBuilder()
        for (i in 0 until arenaResults.size) {
            val matchResult = matchImage(byteBufferImage,
                    rectFactory!!.ARENA_RECTS[i],
                    extractFeatures,
                    computeDistance,
                    candidates,
                    mapping)
            arenaResults[i] = ArenaResult(generatedData.TIERLIST_IDS[matchResult.bestIndex], matchResult.distance)

            sb.append("[" + arenaResults[i]?.cardId + "(" + matchResult.distance + ")]")
        }

        Log.d("Detector", sb.toString())
        return arenaResults as Array<ArenaResult>
    }


    companion object {
        fun hammingDistance(a: Long, b: Long): Double {
            var dist = 0.0

            val c = a xor b
            for (i in 0 until 64) {
                if (c and 1L.shl(i) != 0L) {
                    dist++
                }
            }
            return dist
        }

        fun euclidianDistance(a: DoubleArray, b: DoubleArray): Double {
            var dist = 0.0
            for (i in 0 until a.size) {
                dist += (a[i] - b[i]) * (a[i] - b[i])
            }
            return dist
        }

        fun extractHaar(byteBufferImage: ByteBufferImage, rrect: RRect): DoubleArray {
            return FeatureExtractor.INSTANCE.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, rrect)
        }

        fun extractPhash(byteBufferImage: ByteBufferImage, rrect: RRect): Long {
            return FeatureExtractor.INSTANCE.getHash(byteBufferImage.buffer, byteBufferImage.stride, rrect)
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

        fun <T> getModel(context: Context, resId: Int, clazz: Class<T>): T {
            return Gson().fromJson(InputStreamReader(context.resources.openRawResource(resId)), clazz)
        }

        fun haarToString(features: DoubleArray): String {
            val sb= StringBuilder()

            sb.append("=[")
            sb.append(features.map {String.format("%3.2f", it)}.joinToString(" "))
            sb.append("]")
            return sb.toString()
        }

        fun formatString(format: Int): String {
            return when(format) {
                FORMAT_WILD -> "WILD"
                FORMAT_STANDARD -> "STANDARD"
                else -> "UNKNOWN"
            }
        }

        fun formatMode(mode: Int): String {
            return when(mode) {
                MODE_CASUAL -> "MODE_CASUAL"
                MODE_RANKED -> "MODE_RANKED"
                else -> "UNKNOWN"
            }
        }
    }
}


