package net.mbonnin.arcanetracker.detector

class FeatureExtractor {
    init {
        System.loadLibrary("feature_extractor")
    }

    external fun sayHello(byteBuffer: ByteBuffer): DoubleArray;
}