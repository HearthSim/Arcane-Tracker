package net.mbonnin.arcanetracker.detector

import org.junit.Test

class TestDetector {
    @Test
    fun generateRanks() {
        for(i in 0..25) {
            val pngPath = String.format("/ranks/Medal_Ranked_%d.png", i)
            javaClass.getResourceAsStream(pngPath)


        }
    }

    @Test
    fun testPng() {
        val pngPath = String.format("/screenshots/rank_20.png")
        val inputStream = javaClass.getResourceAsStream(pngPath)

        byteBufferImageToPng(pngToByteBufferImage(inputStream), "./out.png")
    }

    @Test
    fun testScale() {
        val pngPath = String.format("/screenshots/rank_20.png")
        val inputStream = javaClass.getResourceAsStream(pngPath)

        val byteBufferImage = pngToByteBufferImage(inputStream)

        val rRect = RRect(0.0, 0.0, 1.0, 1.0)
        val images = Detector({}).scaleImage(byteBufferImage, rRect, 64, 64)

        atImageToPng(images[0], "./resized.png")
    }
}