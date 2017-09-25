package net.mbonnin.arcanetracker.detector

import android.support.test.InstrumentationRegistry
import android.util.Log
import org.junit.Test
import java.io.FileInputStream

class TestArena {
    var total = 0
    var failed = ArrayList<String>()
    lateinit var detector: Detector

    @Test
    fun doDetect() {
        detector = Detector(InstrumentationRegistry.getTargetContext(), false)

        doTest("/sdcard/tests/arena_choices/heroes.png", "HERO_05", "HERO_06", "HERO_09a")
        doTest("/sdcard/tests/arena_choices/0.png", "EX1_103", "UNG_070", "UNG_083")
        doTest("/sdcard/tests/arena_choices/1.png", "ICC_220", "CS2_213", "AT_092")
        doTest("/sdcard/tests/arena_choices/2.png", "CS1_130", "CFM_648", "OG_327")
        doTest("/sdcard/tests/arena_choices/3.png", "KAR_061", "OG_133", "CFM_344") // this one is golden
        doTest("/sdcard/tests/arena_choices/4.png", "KAR_062", "UNG_937", "OG_048")

        Log.d("TestArena", failed.size.toString() + "/" + total + "(" + (failed.size.toDouble() / total) + ")")
        Log.d("TestArena", failed.joinToString("\n"))
    }

    fun doTest(imagePath: String, vararg id: String) {
        val byteBufferImage = inputStreamToByteBufferImage(FileInputStream(imagePath))
        val result = detector.detectArena(byteBufferImage)

        for (i in 0 until 3) {
            if (result[i] != id[i]) {
                val sb = StringBuilder()

                try {
                    val expectedVector = detector.generatedData.TIERLIST[detector.generatedData.TIERLIST_IDS.indexOf(id[i])]
                    val expectedResult = detector.matchImage(byteBufferImage, ARENA_RECTS[i], arrayOf(expectedVector))

                    sb.append("expected: " + id[i] + " (" + expectedResult.distance + ")")
                    val actualVector = detector.generatedData.TIERLIST[detector.generatedData.TIERLIST_IDS.indexOf(result[i])]
                    val actualResult = detector.matchImage(byteBufferImage, ARENA_RECTS[i], arrayOf(actualVector))
                    sb.append("actual: " + result[i] + " (" + actualResult.distance + ")")
                } catch (e: Exception) {
                    Log.d("TestArena", "", e)
                }

                failed.add(sb.toString())
            }
            total++
        }
    }
}