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

open class RRect(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun scale(sx: Double, sy: Double): RRect = RRect(sx * x, sy * y, sx * w, sy * h)
}

class MatchResult {
    var bestIndex = 0
    var distance = 0.0
}

class ArenaResult(var cardId: String = "", var distance: Double = 0.0)

class ATImage(val w: Int, val h: Int, val buffer: DoubleArray)

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

val FORMAT_RRECT = RRect(1754.0, 32.0, 138.0, 98.0).scale(1 / 1920.0, 1 / 1080.0)
val FORMAT_RRECT_TABLET = RRect(1609.0, 39.0, 96.0, 73.0).scale(1 / 2048.0, 1 / 1536.0)
val RANK_RRECT = RRect(820.0, 424.0, 230.0, 102.0).scale(1 / 1920.0, 1 / 1080.0)
val RANK_RRECT_TABLET = RRect(1730.0, 201.0, 110.0, 46.0).scale(1 / 2048.0, 1 / 1536.0)
val MODE_RRECT = RRect(1270.0, 256.0, 140.0, 32.0).scale(1 / 1920.0, 1 / 1080.0)
val MODE_RRECT_TABLET = RRect(1432.0, 400.0, 160.0, 34.0).scale(1 / 2048.0, 1 / 1536.0)

// beware Y coordinates in inkscape are from the lower left corner
val ARENA_RECTS = arrayOf(
        RRect(344.138, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1 / 1920.0, 1 / 1080.0),
        RRect(854.205, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1 / 1920.0, 1 / 1080.0),
        RRect(1379.876, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1 / 1920.0, 1 / 1080.0)
)

class Detector(var context: Context, var isTablet: Boolean) {
    var mapping: List<Int> = listOf()
    var lastHero: String = ""
    val generatedData = Gson().fromJson(InputStreamReader(context.resources.openRawResource(R.raw.generated_data)), GeneratedData::class.java)

    fun detectRank(byteBufferImage: ByteBufferImage): Int {
        val matchResult = matchImage(byteBufferImage,
                if (isTablet) RANK_RRECT_TABLET else RANK_RRECT,
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

        val matchResult = matchImage(byteBufferImage,
                if (isTablet) FORMAT_RRECT_TABLET else FORMAT_RRECT,
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

        val matchResult = matchImage(byteBufferImage,
                if (isTablet) MODE_RRECT_TABLET else MODE_RRECT,
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                if (isTablet) generatedData.MODES_TABLET else generatedData.MODES)
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
        return detectArena(byteBufferImage, Detector.Companion::extractHaar, Detector.Companion::euclidianDistance, generatedData.TIERLIST_HAAR, hero)
    }

    fun detectArenaPhash(byteBufferImage: ByteBufferImage, hero: String): Array<ArenaResult> {
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
                    ARENA_RECTS[i],
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
            return FeatureExtractor.INSTANCE.getFeatures(byteBufferImage.buffer, byteBufferImage.stride,
                    rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()))
        }

        fun extractPhash(byteBufferImage: ByteBufferImage, rrect: RRect): Long {
            return FeatureExtractor.INSTANCE.getHash(byteBufferImage.buffer, byteBufferImage.stride,
                    rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()))
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

    }
}


