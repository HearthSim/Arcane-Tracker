package net.hearthsim.hsreplay

import okio.GzipSink
import okio.Okio
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual object GzipEncoder {
    actual fun encode(bytes: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val gzipSink = Okio.buffer(GzipSink(Okio.sink(byteArrayOutputStream)))
        val source = Okio.buffer(Okio.source(ByteArrayInputStream(bytes)))
        gzipSink.writeAll(source)
        gzipSink.flush()
        gzipSink.close()

        return byteArrayOutputStream.toByteArray()
    }
}