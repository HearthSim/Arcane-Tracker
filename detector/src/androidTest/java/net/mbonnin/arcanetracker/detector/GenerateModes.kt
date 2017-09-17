package net.mbonnin.arcanetracker.detector


import android.util.Log
import org.junit.Test

class GenerateModes {
    @Test
    fun generateModes() {
        val l = ArrayList<String>()

        val featureDetector = FeatureExtractor()

        for(i in arrayOf("casual", "ranked")) {
            val pngPath = String.format("/screenshots/%s.png", i)

            val byteBufferImage = pngToByteBufferImage(javaClass.getResourceAsStream(pngPath))

            val vector = featureDetector.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, MODE_IN_XP, MODE_IN_YP, MODE_IN_WP, MODE_IN_HP)

            l.add("doubleArrayOf(" + vector.joinToString(",") + ")")
        }

        for (line in l) {
            Log.e("MODE", line)
        }
    }
}