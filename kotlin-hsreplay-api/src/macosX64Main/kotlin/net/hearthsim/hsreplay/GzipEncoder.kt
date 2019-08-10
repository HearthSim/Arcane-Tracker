package net.hearthsim.hsreplay

import kotlinx.cinterop.*
import platform.darwin.COMPRESSION_ZLIB
import platform.darwin.compression_encode_buffer
import platform.posix.size_t
import platform.posix.uint8_tVar

actual object GzipEncoder {
    actual fun encode(bytes: ByteArray): ByteArray {
        val src = nativeHeap.allocArrayOf(bytes).reinterpret<uint8_tVar>()
        val dst = nativeHeap.allocArray<uint8_tVar>(bytes.size)
        val count = compression_encode_buffer(
                dst,
                bytes.size.convert(),
                src,
                bytes.size.convert(),
                null,
                COMPRESSION_ZLIB)

        val result = dst.readBytes(count.convert())

        nativeHeap.free(src)
        nativeHeap.free(dst)

        return result
    }
}