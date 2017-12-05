package net.mbonnin.arcanetracker.detector

import android.util.Log

class OneSecondMinimum(val threshold: Double = 5.0, val tag: String? = null) {
    val WINDOW = 60
    val indices = IntArray(WINDOW, { -1 })
    val distances = DoubleArray(WINDOW, { Double.MAX_VALUE })
    var sample = 0

    fun detect(byteBufferImage: ByteBufferImage, rRect: RRect, candidates: List<DoubleArray>): Int {
        val matchResult = Detector.matchImage(byteBufferImage,
                rRect,
                Detector.Companion::extractHaar,
                Detector.Companion::euclidianDistance,
                candidates)

        if (tag != null) {
            Log.d(tag, "" + matchResult.bestIndex + "(" + matchResult.distance + ")")
        }

        indices[sample] = matchResult.bestIndex
        distances[sample] = matchResult.distance

        sample = (sample + 1) % WINDOW

        var minDistance = Double.MAX_VALUE
        var bestIndex = -1
        for (i in 0 until WINDOW) {
            if (distances[i] < minDistance) {
                minDistance = distances[i]
                bestIndex = i
            }
        }

        return if (minDistance > threshold) INDEX_UNKNOWN else indices[bestIndex]
    }
}