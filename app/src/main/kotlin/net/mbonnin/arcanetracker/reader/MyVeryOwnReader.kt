package net.mbonnin.arcanetracker.reader

import timber.log.Timber
import java.io.*
import java.nio.charset.Charset


/**
 * our own implementation of BufferedReader, etc... Mainly because I don't trust these classes to do the appropriate thing
 * when reading continuously from the end of file.... Maybe I'm wrong but I have to try to get a better understanding at all this
 */
class MyVeryOwnReader @Throws(FileNotFoundException::class)
constructor(val inputStream: InputStream) {
    internal var buffer = ByteArray(16 * 1024)
    internal var currentLine = ByteArray(16 * 1024)
    internal var lineOffset: Int = 0
    private var bufferMax: Int = 0
    private var bufferRead: Int = 0

    @Throws(IOException::class)
    fun skip(count: Long) {
        inputStream.skip(count)
        bufferMax = 0
        bufferRead = 0
        lineOffset = 0
    }

    fun close() {
        try {
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * This assumes ascii charset
     */
    @Throws(IOException::class)
    fun readLine(): String? {
        while (true) {
            if (bufferRead == bufferMax) {
                val ret = inputStream.read(buffer)
                if (ret == -1) {
                    return null
                } else if (ret == 0) {
                    Timber.e("0 bytes read")
                    return null
                }
                bufferMax = ret
                bufferRead = 0
            }

            while (bufferRead < bufferMax) {
                val b = buffer[bufferRead++]

                if (b == '\n'.toByte()) {
                    val line = String(currentLine, 0, lineOffset, Charset.forName("utf-8"))
                    lineOffset = 0
                    return line
                } else {
                    if (lineOffset >= currentLine.size) {
                        currentLine = currentLine.copyOf(lineOffset * 2)

                    }
                    currentLine[lineOffset++] = b
                }
            }
        }
    }
}
