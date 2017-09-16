package net.mbonnin.arcanetracker.detector

import org.junit.Test

class GreenTest {
    @Test
    fun testFeatures() {
        val pngPath = String.format("/features/green.png")
        val inputStream = javaClass.getResourceAsStream(pngPath)

        val byteBufferImage = pngToByteBufferImage(inputStream)

        val features = FeatureExtractor().getFeatures(byteBufferImage.buffer, byteBufferImage.w.toDouble()/2, 0.toDouble(), byteBufferImage.w.toDouble()/2, byteBufferImage.h.toDouble(), byteBufferImage.stride)

        System.out.print(features[0])
    }
}
