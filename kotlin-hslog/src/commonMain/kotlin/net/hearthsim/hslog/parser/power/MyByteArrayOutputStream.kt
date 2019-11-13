package net.hearthsim.hslog.parser.power

class MyByteArrayOutputStream {
    companion object {
        const val INITIAL_SIZE = 500 * 1024
        const val GROW_SIZE = 500 * 1024
    }

    private var buffer = ByteArray(INITIAL_SIZE)
    private var written = 0

    fun clear() {
        buffer = ByteArray(INITIAL_SIZE)
        written = 0
    }
    fun write(bytes: ByteArray) {
        while (buffer.size < written + bytes.size) {
            buffer = buffer.copyOf(written + bytes.size + GROW_SIZE)
        }
        bytes.copyInto(buffer, written, 0, bytes.size)
        written += bytes.size
    }

    fun bytes(): ByteArray {
        return buffer.copyOf(written)
    }
}