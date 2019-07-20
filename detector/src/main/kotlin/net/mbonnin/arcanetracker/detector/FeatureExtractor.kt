package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class FeatureExtractor {
    init {
        System.loadLibrary("feature_extractor")
    }

    /*
     * x, y, w, h represent the input rect where to compute the features
     */
    external private fun getFeatures(byteBuffer: ByteBuffer, stride: Int, x: Double, y: Double, w: Double, h: Double): DoubleArray

    external private fun getHash(byteBuffer: ByteBuffer, stride: Int, x: Double, y: Double, w: Double, h: Double): Long


    fun getFeatures(byteBuffer: ByteBuffer, stride: Int, rrect: RRect): DoubleArray {
        return getFeatures(byteBuffer, stride, rrect.x, rrect.y, rrect.w, rrect.h)
    }

    fun getHash(byteBuffer: ByteBuffer, stride: Int, rrect: RRect): Long {
        return getHash(byteBuffer, stride, rrect.x, rrect.y, rrect.w, rrect.h)
    }

    companion object {
        val INSTANCE = FeatureExtractor()
    }
}