package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader
import java.nio.ByteBuffer

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

class ATImage(val w: Int, val h: Int, val buffer: DoubleArray)

const val INDEX_UNKNOWN = -1
const val RANK_UNKNOWN = INDEX_UNKNOWN

const val FORMAT_UNKNOWN = INDEX_UNKNOWN
const val FORMAT_WILD = 0
const val FORMAT_STANDARD =1

const val MODE_UNKNOWN = INDEX_UNKNOWN
const val MODE_CASUAL_STANDARD = 0
const val MODE_CASUAL_WILD = 1
const val MODE_RANKED_STANDARD = 2
const val MODE_RANKED_WILD = 3

val FORMAT_RRECT = RRect(1754.0, 32.0, 138.0, 98.0).scale(1/1920.0, 1/1080.0)
val FORMAT_RRECT_TABLET = RRect(1609.0, 39.0, 96.0, 73.0).scale(1/2048.0, 1/1536.0)
val RANK_RRECT = RRect(820.0, 424.0, 230.0, 102.0).scale(1/1920.0, 1/1080.0)
val RANK_RRECT_TABLET = RRect(1730.0, 201.0, 110.0, 46.0).scale(1/2048.0, 1/1536.0)
val MODE_RRECT = RRect(1270.0, 256.0, 140.0, 32.0).scale(1/1920.0, 1/1080.0)
val MODE_RRECT_TABLET = RRect(1432.0, 400.0, 160.0, 34.0).scale(1/2048.0, 1/1536.0)

// beware Y coordinates in inkscape are from the lower left corner
val ARENA_RECTS = arrayOf(
        RRect(344.138, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1/1920.0, 1/1080.0),
        RRect(854.205, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1/1920.0, 1/1080.0),
        RRect(1379.876, 1080.0 - 642.198 - 187.951, 185.956, 187.951).scale(1/1920.0, 1/1080.0)
)

class Detector(var context: Context, var isTablet: Boolean) {
    val featureDetector = FeatureExtractor()
    val matchResult = MatchResult()
    val arenaResult = Array<String>(3, {""})
    val generatedData = Gson().fromJson(InputStreamReader(context.resources.openRawResource(R.raw.generated_data)), GeneratedData::class.java)

    fun matchImage(byteBufferImage: ByteBufferImage,  rrect: RRect, candidates: Array<DoubleArray>):MatchResult {

        val vector = featureDetector.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()));

        var index = 0
        matchResult.bestIndex = INDEX_UNKNOWN
        matchResult.distance = Double.MAX_VALUE

        for (rankVector in candidates) {
            var dist = 0.0
            for (i in 0 until vector.size) {
                dist += (vector[i] - rankVector[i]) * (vector[i] - rankVector[i])
            }
            //Log.d("Detector", String.format("%d: %f", rank, dist))
            if (dist < matchResult.distance) {
                matchResult.distance = dist
                matchResult.bestIndex = index
            }
            index++
        }
        return matchResult
    }

    fun detectRank(byteBufferImage: ByteBufferImage):Int {
        val matchResult = matchImage(byteBufferImage, if (isTablet) RANK_RRECT_TABLET else RANK_RRECT , generatedData.RANKS)

        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "rank: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectFormat(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, if (isTablet) FORMAT_RRECT_TABLET else FORMAT_RRECT, generatedData.FORMATS)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectMode(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, if (isTablet) MODE_RRECT_TABLET else MODE_RRECT, if (isTablet) generatedData.MODES_TABLET else generatedData.MODES)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "mode: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectArena(byteBufferImage: ByteBufferImage):Array<String> {
        for (i in 0 until arenaResult.size) {
            val matchResult = matchImage(byteBufferImage, ARENA_RECTS[i], generatedData.TIERLIST)
            arenaResult[i] = generatedData.TIERLIST_IDS[matchResult.bestIndex]

        }

        Log.d("Detector", String.format("[%s][%s][%s]", arenaResult[0], arenaResult[1], arenaResult[2]))

        return arenaResult;
    }
}


