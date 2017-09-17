
import android.content.Intent
import android.support.test.InstrumentationRegistry
import net.mbonnin.arcanetracker.detector.*
import org.junit.Test
import java.util.*

val MEDAL_RRECT = RRect(24.0, 82.0, 209.0, 92.0).scale(1/256.0, 1/256.0)

class GenerateData {
    @Test
    fun generateData() {
        val featureExtractor = FeatureExtractor()

        val sb = StringBuilder()

        sb.append("package net.mbonnin.arcanetracker.detector\n");

        val range = 0..25
        appendData(sb, "RANKS", range.map {String.format("/ranks/Medal_Ranked_%d.png", it)}, featureExtractor, MEDAL_RRECT)
        appendData(sb, "FORMATS", listOf("wild", "standard").map {String.format("/screenshots/%s.png", it)}, featureExtractor, FORMAT_RRECT)
        appendData(sb, "MODES", listOf("casual", "ranked").map {String.format("/screenshots/%s.png", it)}, featureExtractor, MODE_RRECT)

        // lol, that's the simplest way I found to send the generated file to the outside world without requiring write permissions !
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/plain"
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@arcanetracker.com"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Data.kt")
        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString())

        InstrumentationRegistry.getContext().startActivity(emailIntent)
    }

    private fun appendData(sb: StringBuilder, s: String, fileList: List<String>, featureDetector: FeatureExtractor, rrect: RRect) {
        sb.append(String.format(Locale.ENGLISH, "val %s = arrayOf(\n", s))

        val l = ArrayList<String>()
        for(file in fileList) {

            val byteBufferImage = pngToByteBufferImage(javaClass.getResourceAsStream(file))

            val vector = featureDetector.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()));

            l.add("doubleArrayOf(" + vector.joinToString(", ") + ")")
        }
        sb.append(l.joinToString(",\n"))
        sb.append("\n)\n")

    }
}