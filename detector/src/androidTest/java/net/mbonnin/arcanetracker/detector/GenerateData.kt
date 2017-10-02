
import android.Manifest
import android.graphics.BitmapFactory
import android.support.test.InstrumentationRegistry
import android.support.test.rule.GrantPermissionRule
import android.util.Log
import com.google.gson.Gson
import net.mbonnin.arcanetracker.detector.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


val MEDAL_RRECT = RRect(24.0, 82.0, 209.0, 92.0).scale(1 / 256.0, 1 / 256.0)

// see superposition.svg for CARD and HERO rects
val CARD_RECT = RRect(
        x = 4906.075 - 4766.787, // change 4906 here
        y = 718.251 + 512.00 - 890.654 - 226.622, // inkscape y coordinates starts in the bottom left corner
        w = 224.216,
        h = 226.622).scale(1 / 512.0, 1 / 512.0)

val HERO_RECT = RRect(
        x = 2354.645 - 2283.807, // change 4906 here
        y = 2370.551 + 345.000 - 2625.288 - 84.009, // inkscape y coordinates starts in the bottom left corner
        w = 83.117,
        h = 84.009).scale(1 / 250.0, 1 / 345.0)

class GenerateData {
    @Rule
    @JvmField
    val permissionsRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @Test
    fun generateData() {
        val featureExtractor = FeatureExtractor()

        val range = 0..25

        val ranks = getVectorArray(range.map { String.format("/medals/Medal_Ranked_%d.png", it) }, featuresCallback(featureExtractor, MEDAL_RRECT))
        val formats = getVectorArray(listOf("wild", "standard").map { String.format("/formats/%s.png", it) }, featuresCallback(featureExtractor, FORMAT_RRECT))
        val modes = getVectorArray(listOf(
                "casual_standard",
                "casual_wild",
                "ranked_standard",
                "ranked_wild")
                .map {
                    String.format("/modes/%s.png", it)
                }, featuresCallback(featureExtractor, MODE_RRECT))
        val modes_tablet = getVectorArray(listOf(
                "casual_standard_tablet",
                "casual_wild_tablet",
                "ranked_standard_tablet",
                "ranked_wild_tablet")
                .map {
                    String.format("/modes/%s.png", it)
                }, featuresCallback(featureExtractor, MODE_RRECT_TABLET))

        val arena = appendArena(featureExtractor);

        val generatedData = GeneratedData(ranks, formats, modes, modes_tablet, arena.first, arena.second, arena.third)

        // lol, that's the simplest way I found to send the generated file to the outside world without requiring write permissions !
        val okhttpClient = OkHttpClient()

        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_dev_key", "b8c2369b8462bde94ccf17bd950d9555")
                .addFormDataPart("api_option", "paste")
                .addFormDataPart("api_paste_code", Gson().toJson(generatedData))
                .build()

        val request = Request.Builder()
                .url("https://pastebin.com/api/api_post.php")
                .post(requestBody)
                .build()
        val response = okhttpClient.newCall(request).execute()

        Log.d("TAG", "data is at: " + response.body()?.string())

        FileOutputStream(File("/sdcard/generated_data.json")).write(Gson().toJson(generatedData).toByteArray())
    }

    private fun appendArena(featureExtractor: FeatureExtractor): Triple<List<String>, List<DoubleArray>, List<Long>> {

        val list = Tierlist.get(InstrumentationRegistry.getTargetContext()).list

        val cardIds = list.map { it.CardId }
        val vectors = ArrayList<DoubleArray>()
        val hashes = ArrayList<Long>()

        val ids = ArrayList<String>()

        ids.addAll(cardIds)

        val heroes = listOf(
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
                "KARA_00_03H"
        )

        //vectors.addAll(getVectorArray(ids.map { "/cards/" + it + ".jpg" }, featuresCallback(featureExtractor, CARD_RECT)))
        //vectors.addAll(getVectorArray(heroes.map { "/heroes/A" + it + ".png" }, featuresCallback(featureExtractor, HERO_RECT)))

        hashes.addAll(getVectorArray(ids.map { "/cards/" + it + ".jpg" }, phashCallback(featureExtractor, CARD_RECT)))
        hashes.addAll(getVectorArray(heroes.map { "/heroes/A" + it + ".png" },  phashCallback(featureExtractor, HERO_RECT)))

        ids.addAll(heroes)

        return Triple(ids, vectors, hashes)
    }

    private fun featuresCallback(featureExtractor: FeatureExtractor, rrect: RRect): (ByteBufferImage) -> DoubleArray {
        return { byteBufferImage ->
            featureExtractor.getFeatures(byteBufferImage.buffer, byteBufferImage.stride,
                    rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble())).copyOf()
        }
    }

    private fun phashCallback(featureExtractor: FeatureExtractor, rrect: RRect): (ByteBufferImage) -> Long {
        return { byteBufferImage ->
            featureExtractor.getHash(byteBufferImage.buffer, byteBufferImage.stride,
                    rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()))
        }
    }

    private fun <T> getVectorArray(fileList: List<String>, apply: (ByteBufferImage) -> T): ArrayList<T> {
        val vectors = ArrayList<T>()

        var i = 0
        for (fileName in fileList) {
            Log.d("TAG", i++.toString() + "/" + fileList.size + ":" + fileName)

            try {
                val inputStream = FileInputStream(File("/sdcard/models" + fileName))
                val bm = BitmapFactory.decodeStream(inputStream)
                val byteBufferImage = bitmapToByteBufferImage(bm!!)

                val vector = apply(byteBufferImage)

                vectors.add(vector)
            } catch (e: Exception) {
                Log.e("TAG", "oops", e)
            }
        }

        return vectors
    }
}