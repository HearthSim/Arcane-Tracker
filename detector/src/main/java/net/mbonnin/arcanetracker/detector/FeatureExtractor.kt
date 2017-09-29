package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class FeatureExtractor {
    init {
        System.loadLibrary("feature_extractor")
    }

    private var vector: DoubleArray

    /*
     * x, y, w, h represent the input rect where to compute the features
     */
    external private fun getFeatures(byteBuffer: ByteBuffer, stride: Int, x: Double, y: Double, w: Double, h: Double, features: DoubleArray)

    external private fun allocateVector(): DoubleArray


    constructor() {
        vector = allocateVector()
    }

    fun getFeatures(byteBuffer: ByteBuffer, stride: Int, rrect: RRect): DoubleArray {
        getFeatures(byteBuffer, stride, rrect.x, rrect.y, rrect.w, rrect.h, vector)
        return vector
    }
}