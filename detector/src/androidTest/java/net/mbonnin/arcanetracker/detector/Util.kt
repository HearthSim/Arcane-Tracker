package net.mbonnin.arcanetracker.detector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.InputStream
import java.nio.ByteBuffer

fun bitmapToByteBufferImage(bm: Bitmap): ByteBufferImage {
    val buffer = ByteBuffer.allocateDirect(bm.width * bm.height * 4)
    for (j in 0 until bm.height) {
        for (i in 0 until bm.width) {
            val pixel = bm.getPixel(i, j)
            buffer.put(Color.red(pixel).toByte())
            buffer.put(Color.green(pixel).toByte())
            buffer.put(Color.blue(pixel).toByte())
            buffer.put(0xff.toByte())
        }
    }
    return ByteBufferImage(bm.width, bm.height, buffer, bm.width * 4)
}

fun inputStreamToByteBufferImage(inputStream: InputStream): ByteBufferImage {
    val bm = BitmapFactory.decodeStream(inputStream)
    return bitmapToByteBufferImage(bm)
}