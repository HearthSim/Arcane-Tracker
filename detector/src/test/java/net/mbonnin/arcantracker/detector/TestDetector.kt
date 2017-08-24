package net.mbonnin.arcantracker.detector

import ar.com.hjg.pngj.PngReader
import org.junit.Test

class TestDetector {
    @Test
    fun generateRanks() {
        for(i in 0..25) {
            val pngPath = String.format("/ranks/Medal_Ranked_%d.png", i)
            val inputStream = javaClass.getResourceAsStream(pngPath)
            PngReader(inputStream)

        }
    }
}