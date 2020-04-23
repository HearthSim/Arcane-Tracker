package net.mbonnin.jolly

import kotlinx.cinterop.*
import platform.zlib.compress
import platform.posix.size_t
import platform.posix.uint8_tVar
import platform.zlib.uLongfVar

actual object GzipEncoder {
    actual fun encode(bytes: ByteArray): ByteArray {
        return memScoped {
            val src = allocArrayOf(bytes).reinterpret<uint8_tVar>()
            val dst = allocArray<uint8_tVar>(bytes.size)

            val size = alloc<uLongfVar>()
            size.value = bytes.size.convert()

            val count = compress(
                    dst,
                    size.ptr,
                    src,
                    bytes.size.convert())

            val result = dst.readBytes(count.convert())

            result
        }
    }
}