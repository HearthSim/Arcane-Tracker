package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class FeatureExtractor {
    init {
        System.loadLibrary("feature_extractor")
    }

    /*
     * x, y, w, h represent the input rect where to compute the features
     */
    external fun getFeatures(byteBuffer: ByteBuffer, x: Double, y: Double, w: Double, h: Double, stride: Int): DoubleArray;
}