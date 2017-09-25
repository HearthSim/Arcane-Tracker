
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
import java.util.*


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
    @Rule @JvmField
    val permissionsRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    @Test
    fun generateData() {
        val featureExtractor = FeatureExtractor()

        val range = 0..25

        val ranks = getVectorArray(range.map { String.format("/medals/Medal_Ranked_%d.png", it) }, featureExtractor, MEDAL_RRECT)
        val formats = getVectorArray(listOf("wild", "standard").map { String.format("/formats/%s.png", it) }, featureExtractor, FORMAT_RRECT)
        val modes = getVectorArray(listOf(
                "casual_standard",
                "casual_wild",
                "ranked_standard",
                "ranked_wild")
                .map {
                    String.format("/modes/%s.png", it)
                }, featureExtractor, MODE_RRECT)
        val modes_tablet = getVectorArray(listOf(
                "casual_standard_tablet",
                "casual_wild_tablet",
                "ranked_standard_tablet",
                "ranked_wild_tablet")
                .map {
                    String.format("/modes/%s.png", it)
                }, featureExtractor, MODE_RRECT_TABLET)

        val arena = appendArena(featureExtractor);

        val generatedData = GeneratedData(ranks, formats, modes, modes_tablet, arena.second, arena.first)

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

    private fun appendArena(featureExtractor: FeatureExtractor): Pair<Array<String>, Array<DoubleArray>> {


        val list = Tierlist.get(InstrumentationRegistry.getTargetContext()).list

        val ids = ArrayList<String>()
        val vectors = ArrayList<DoubleArray>()

        ids.addAll(list
                .map { it.CardId }
                )
        vectors.addAll(getVectorArray(ids.map { "/cards/" + it + ".jpg" }, featureExtractor, CARD_RECT))

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

        vectors.addAll(getVectorArray(heroes.map { "/heroes/A" + it + ".png" }, featureExtractor, HERO_RECT))
        ids.addAll(heroes)

        return Array(ids.size, { ids.get(it) }) to Array(vectors.size, { vectors.get(it) })
    }


    private fun getVectorArray(fileList: List<String>, featureExtractor: FeatureExtractor, rrect: RRect): Array<DoubleArray> {
        val vectors = arrayListOf<DoubleArray>()

        var i = 0
        for (fileName in fileList) {
            Log.d("TAG", i++.toString() + "/" + fileList.size + ":" + fileName)

            try {
                val inputStream = FileInputStream(File("/sdcard/models" + fileName))
                val bm = BitmapFactory.decodeStream(inputStream)
                val byteBufferImage = bitmapToByteBufferImage(bm!!)

                val vector = featureExtractor.getFeatures(byteBufferImage.buffer, byteBufferImage.stride,
                        rrect.scale(byteBufferImage.w.toDouble(), byteBufferImage.h.toDouble()));

                vectors.add(vector.copyOf())
            } catch (e: Exception) {
                Log.e("TAG", "oops", e)
            }
        }

        return Array<DoubleArray>(vectors.size, { vectors.get(it) })
    }
}