package net.mbonnin.arcanetracker.detector


import android.util.Log
import org.junit.Test

class GenerateFormats {
    @Test
    fun generateFormat() {
        val l = ArrayList<String>()

        val featureDetector = FeatureExtractor()

        for(i in arrayOf("wild", "standard")) {
            val pngPath = String.format("/screenshots/%s.png", i)

            val byteBufferImage = pngToByteBufferImage(javaClass.getResourceAsStream(pngPath))

            val vector = featureDetector.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, FORMAT_IN_XP, FORMAT_IN_YP, FORMAT_IN_WP, FORMAT_IN_HP)

            l.add("doubleArrayOf(" + vector.joinToString(",") + ")")
        }

        for (line in l) {
            Log.e("FORMAT", line)
        }
    }
}