package net.mbonnin.jolly

expect object GzipEncoder {
    fun encode(bytes: ByteArray): ByteArray
}
