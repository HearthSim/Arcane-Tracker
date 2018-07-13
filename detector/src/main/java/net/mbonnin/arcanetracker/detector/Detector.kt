package net.mbonnin.arcanetracker.detector

import android.content.Context
import com.squareup.moshi.Moshi
import okio.Okio
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

const val INDEX_UNKNOWN = -1
const val RANK_UNKNOWN = INDEX_UNKNOWN

class Detector(var context: Context, val isTablet: Boolean) {
    private val moshi = Moshi.Builder()
            .build()

    val generatedData by lazy {
        val adapter = moshi.adapter(RankData::class.java)
        val bufferedSource = Okio.buffer(Okio.source(this::class.java.getResourceAsStream("/rank_data.json")))
        bufferedSource.use {
            adapter.fromJson(bufferedSource)
        }!!
    }
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

    companion object {
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
    }
}


