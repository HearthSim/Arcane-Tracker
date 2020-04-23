package net.mbonnin.jolly

import okio.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual object GzipEncoder {
    actual fun encode(bytes: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipSink = GzipSink(byteArrayOutputStream.sink()).buffer()
        val source = ByteArrayInputStream(bytes).source().buffer()
        gzipSink.writeAll(source)
        gzipSink.flush()
        gzipSink.close()

        return byteArrayOutputStream.toByteArray()
    }
}