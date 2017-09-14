package net.mbonnin.arcanetracker.detector

import java.nio.ByteBuffer

class FeatureExtractor {
    init {
        System.loadLibrary("feature_extractor")
    }

    external fun getFeatures(byteBuffer: ByteBuffer, w: Int, h: Int, stride: Int): DoubleArray;
}