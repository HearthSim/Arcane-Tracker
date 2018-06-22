package net.mbonnin.arcanetracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer

object ByteBufferImageFactory {
    fun create(bm: Bitmap): ByteBufferImage {
        val buffer = ByteBuffer.allocateDirect(bm.width * bm.height * 4)
        val intBuffer = buffer.asIntBuffer()
        for (j in 0 until bm.height) {
            for (i in 0 until bm.width) {
                val pixel = bm.getPixel(i, j)

                // bitmap is ARGB but the screen capture stuff is RGBA
                intBuffer.put(pixel.shl(8))
            }
        }
        return ByteBufferImage(bm.width, bm.height, buffer, bm.width * 4)
    }

    fun create(inputStream: InputStream): ByteBufferImage {
        val bm = BitmapFactory.decodeStream(inputStream)
        return create(bm)
    }

    fun create(path: String): ByteBufferImage {
        val inputStream = FileInputStream(path)
        return create(inputStream)
    }
}
