import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.support.test.InstrumentationRegistry
import android.util.Log
import net.mbonnin.arcanetracker.detector.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.nio.ByteBuffer
import java.util.*


val MEDAL_RRECT = RRect(24.0, 82.0, 209.0, 92.0).scale(1 / 256.0, 1 / 256.0)

// see superposition.svg for CARD and HERO rects
val CARD_RECT = RRect(
        x = 4906.075 - 4766.787, // change 4906 here
        y = 718.251 + 512.00 - 890.654, // inkscape y coordinates starts in the bottom left corner
        w = 224.216,
        h = 226.622)

val HERO_RECT = RRect(
        x = 2354.645 - 2283.807, // change 4906 here
        y = 2370.551 + 345.000 - 2625.288, // inkscape y coordinates starts in the bottom left corner
        w = 83.117,
        h = 84.009)

class GenerateData {
    @Test
    fun generateData() {
        val featureExtractor = FeatureExtractor()

        val sb = StringBuilder()

        sb.append("package net.mbonnin.arcanetracker.detector\n");

        val range = 0..25
        appendData(sb, "RANKS", range.map { String.format("/medals/Medal_Ranked_%d.png", it) }, featureExtractor, MEDAL_RRECT)
        appendData(sb, "FORMATS", listOf("wild", "standard").map { String.format("/formats/%s.png", it) }, featureExtractor, FORMAT_RRECT)
        appendData(sb, "MODES", listOf(
                "casual_standard",
                "casual_wild",
                "ranked_standard",
                "ranked_wild")
                .map {
                    String.format("/modes/%s.png", it)
                }, featureExtractor, MODE_RRECT)
        appendData(sb, "MODES_TABLET", listOf(
                "casual_standard_tablet",
                "casual_wild_tablet",
                "ranked_standard_tablet",
                "ranked_wild_tablet")
                .map {
                    String.format("/modes/%s.png", it)
                }, featureExtractor, MODE_RRECT_TABLET)

        appendArena(sb, featureExtractor);

        // lol, that's the simplest way I found to send the generated file to the outside world without requiring write permissions !
        val okhttpClient = OkHttpClient()

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_dev_key", "b8c2369b8462bde94ccf17bd950d9555")
                .addFormDataPart("api_option", "paste")
                .addFormDataPart("api_paste_code", sb.toString())
                .build()

        val request = Request.Builder()
                .url("https://pastebin.com/api/api_post.php")
                .post(requestBody)
                .build()
        val response = okhttpClient.newCall(request).execute()

        Log.d("TAG", "data is at: " + response.body()?.string())
    }

    private fun appendArena(sb: StringBuilder, featureExtractor: FeatureExtractor) {


        val list = Tierlist.get(InstrumentationRegistry.getTargetContext()).list

        var i = 0;
        val ids = ArrayList<String>()
        val vectors = ArrayList<String>()
        for (tierCard in list) {
            Log.d("TAG", i++.toString() + "/" + list.size + ":" + tierCard.CardId)
            try {
                val inputStream = javaClass.getResourceAsStream("/cards/" + tierCard.CardId + ".png")
                val bm = BitmapFactory.decodeStream(inputStream)
                val byteBufferImage = bitmapToByteBufferImage(bm!!)

                val vector = featureExtractor.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, CARD_RECT);

                ids.add(tierCard.CardId)
                vectors.add("doubleArrayOf(" + vector.joinToString(", ") + ")")

            } catch (e: Exception) {
                Log.e("TAG", "error", e)
            }
        }

        val heroes = arrayOf(
                "HERO_01",
                "HERO_01a",
                "HERO_02",
                "HERO_02a",
                "HERO_03",
                "HERO_03a",
                "HERO_04",
                "HERO_04a",
                "HERO_04b",
                "HERO_05",
                "HERO_05a",
                "HERO_06",
                "HERO_07",
                "HERO_08",
                "HERO_08b",
                "HERO_09",
                "HERO_09a",
                "AKARA_00_03H"
                )

        for (hero in heroes) {
            Log.d("TAG", i++.toString() + "/" + list.size + ":" + hero)
            try {
                val inputStream = javaClass.getResourceAsStream("/heroes/" + hero + ".png")
                val bm = BitmapFactory.decodeStream(inputStream)
                val byteBufferImage = bitmapToByteBufferImage(bm!!)

                val vector = featureExtractor.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, HERO_RECT);

                ids.add(hero)
                vectors.add("doubleArrayOf(" + vector.joinToString(", ") + ")")

            } catch (e: Exception) {
                Log.e("TAG", "error", e)
            }
        }

        sb.append("val TIERLIST_VECTORS = mapOf(\n")
        sb.append(vectors.joinToString(",\n"))
        sb.append("\n)\n")
        sb.append("val TIERLIST_IDS = arrayOf(\n")
        sb.append(ids.joinToString(",\n"))
        sb.append("\n)\n")
    }

    private fun bitmapToByteBufferImage(bm: Bitmap): ByteBufferImage {
        val buffer = ByteBuffer.allocateDirect(bm.width * bm.height * 4)
        for (i in 0 until bm.width) {
            for (j in 0 until bm.height) {
                val pixel = bm.getPixel(i, j)
                buffer.put(Color.red(pixel).toByte())
                buffer.put(Color.green(pixel).toByte())
                buffer.put(Color.blue(pixel).toByte())
                buffer.put(0xff.toByte())
            }
        }
        return ByteBufferImage(bm.width, bm.height, buffer, bm.width * 4)
    }

    private fun appendData(sb: StringBuilder, s: String, fileList: List<String>, featureExtractor: FeatureExtractor, rrect: RRect) {
        sb.append(String.format(Locale.ENGLISH, "val %s = arrayOf(\n", s))

        val l = ArrayList<String>()
        for (fileName in fileList) {

            val byteBufferImage = pngToByteBufferImage(javaClass.getResourceAsStream("/models" + fileName))

            val vector = featureExtractor.getFeatures(byteBufferImage.buffer, byteBufferImage.stride, rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()));

            l.add("doubleArrayOf(" + vector.joinToString(", ") + ")")
        }
        sb.append(l.joinToString(",\n"))
        sb.append("\n)\n")

    }
}