package net.hearthsim.hslog

import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import net.hearthsim.hsmodel.CardJson
import okio.Buffer
import platform.posix.read

fun cardJson(fd: Int): CardJson {
    val buffer = Buffer()
    memScoped {
        val byteArray = ByteArray(64*1024)

        while (true) {
            val r = read(fd, byteArray.refTo(0), (64 * 1024).convert())
            if (r.convert<Int>() == 0) {
                break
            }

            if (r < 0) {
                throw Exception("Exception while reading file: $r")
            }
            buffer.write(byteArray, 0, r.convert())
        }
    }

    return CardJson.fromLocalizedJson(buffer)
}