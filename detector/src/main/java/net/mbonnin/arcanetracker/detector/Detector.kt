package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class ByteBufferImage(val w: Int, val h: Int, val buffer: ByteBuffer, val stride: Int) {}

class RRect(val l: Double, val t: Double, val w: Double, val h: Double) {}

class MatchResult {
    var bestIndex = 0
    var distance = 0.0
}

class ATImage(val w: Int, val h: Int, val buffer: DoubleArray) {
    fun getPixel(x: Int, y: Int): Double {
        return buffer.get(x + y * w);
    }
}

const val INDEX_UNKNOWN = -1
const val RANK_UNKNOWN = INDEX_UNKNOWN

const val FORMAT_UNKNOWN = INDEX_UNKNOWN
const val FORMAT_WILD = 0
const val FORMAT_STANDARD =1

const val MODE_UNKNOWN = INDEX_UNKNOWN
const val MODE_CASUAL = 0
const val MODE_RANKED = 1

const val FORMAT_IN_XP = 1754.0
const val FORMAT_IN_YP = 32.0
const val FORMAT_IN_WP = 138.0
const val FORMAT_IN_HP = 98.0

const val MODE_IN_XP = 1270.0
const val MODE_IN_YP = 256.0
const val MODE_IN_WP = 140.0
const val MODE_IN_HP = 32.0

class Detector {
    val featureDetector = FeatureExtractor()
    val matchResult = MatchResult()

    fun matchImage(byteBufferImage: ByteBufferImage, in_xp: Double, in_yp: Double, in_wp: Double, in_hp: Double, candidates: Array<DoubleArray>):MatchResult {

        val in_x = byteBufferImage.w * (in_xp / 1920.0)
        val in_y = byteBufferImage.h * (in_yp / 1080.0)
        val in_w = byteBufferImage.w * (in_wp / 1920.0)
        val in_h = byteBufferImage.h * (in_hp / 1080.0)

        val vector = featureDetector.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, in_x, in_y, in_w, in_h);

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
        val matchResult = matchImage(byteBufferImage, 820.0, 424.0, 230.0, 102.0, RANKS)

        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "rank: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectFormat(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, FORMAT_IN_XP, FORMAT_IN_YP, FORMAT_IN_WP, FORMAT_IN_HP, FORMATS)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }

    fun detectMode(byteBufferImage: ByteBufferImage):Int {

        val matchResult = matchImage(byteBufferImage, MODE_IN_XP, MODE_IN_YP, MODE_IN_WP, MODE_IN_HP, MODES)
        if (matchResult.distance > 400) {
            matchResult.bestIndex = INDEX_UNKNOWN
        }

        //Log.d("Detector", "format: " + matchResult.bestIndex + "(" + matchResult.distance +  ")")

        return matchResult.bestIndex;
    }
}


