
import android.util.Log
import net.mbonnin.arcanetracker.detector.FeatureExtractor
import net.mbonnin.arcanetracker.detector.pngToByteBufferImage
import org.junit.Test

class GenerateRanks {
    @Test
    fun generateRanks() {
        val l = ArrayList<String>()

        val featureDetector = FeatureExtractor()

        for(i in 0..25) {
            val pngPath = String.format("/ranks/Medal_Ranked_%d.png", i)

            val byteBufferImage = pngToByteBufferImage(javaClass.getResourceAsStream(pngPath))

            val in_x = byteBufferImage.w * (24.0 / 256.0)
            val in_y = byteBufferImage.h * (82.0 / 256.0)
            val in_w = byteBufferImage.w * (209.0 / 256.0)
            val in_h = byteBufferImage.h * (92.0 / 256.0)
            val features = featureDetector.getFeatures(byteBufferImage.buffer, in_x, in_y, in_w, in_h, byteBufferImage.stride);

            l.add("doubleArrayOf(" + features.joinToString(",") + ")")

        }

        /*val sb = StringBuilder()
        sb.append("val RANKS = arrayOf(\n")
        sb.append(l.joinToString(",\n"))
        sb.append(")\n")*/

        for (line in l) {
            Log.e("RANKS", line)
        }
    }
}