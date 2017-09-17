package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class ByteBufferImage(val w: Int, val h: Int, val buffer: ByteBuffer, val stride: Int) {}

open class RRect(val x: Double, val y: Double, val w: Double, val h: Double) {
    fun scale(sx: Double, sy: Double): RRect {
        return RRect(sx * x, sy * y, sx * w, sy * h)
    }
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
const val MODE_CASUAL = 0
const val MODE_RANKED = 1

val FORMAT_RRECT = RRect(1754.0, 32.0, 138.0, 98.0).scale(1/1920.0, 1/1080.0)
val RANK_RRECT = RRect(820.0, 424.0, 230.0, 102.0).scale(1/1920.0, 1/1080.0)
val MODE_RRECT = RRect(1270.0, 256.0, 140.0, 32.0).scale(1/1920.0, 1/1080.0)

class Detector {
    val featureDetector = FeatureExtractor()
    val matchResult = MatchResult()

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
        val matchResult = matchImage(byteBufferImage, RANK_RRECT, RANKS)

        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "rank: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectFormat(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, FORMAT_RRECT, FORMATS)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectMode(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, MODE_RRECT, MODES)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }
}


