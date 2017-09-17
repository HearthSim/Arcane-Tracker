package net.mbonnin.arcanetracker.detector

import org.junit.Test

class RedTest {
    @Test
    fun testFeatures() {
        val pngPath = String.format("/features/red.png")
        val inputStream = javaClass.getResourceAsStream(pngPath)

        val byteBufferImage = pngToByteBufferImage(inputStream)

        val vector = FeatureExtractor().getFeatures(byteBufferImage.buffer, byteBufferImage.stride, byteBufferImage.w.toDouble()/2, 0.toDouble(), byteBufferImage.w.toDouble()/2, byteBufferImage.h.toDouble())

        System.out.print(vector[0])
    }
}
