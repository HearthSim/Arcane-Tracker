package net.mbonnin.arcantracker.detector

import org.jtransforms.dct.DoubleDCT_2D
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ByteBufferImage(val w: Int, val h: Int, val buffer: ByteBuffer, val stride: Int) {}

class RRect(val l: Double, val t: Double, val w: Double, val h: Double) {}

class ATImage(val w: Int, val h: Int, val buffer: DoubleArray) {
    fun getPixel(x: Int, y: Int): Double {
        return buffer.get(x + y * w);
    }
}


class Detector(val debugCallback: (ATImages: Array<ATImage>) -> Unit) {

    fun detectRank(byteBufferImage: ByteBufferImage, rRect: RRect) {
        val vector = getRankVectorDCT(byteBufferImage, rRect)
        val minDistance = Double.MAX_VALUE
    }

    fun getRankVectorDCT(byteBufferImage: ByteBufferImage, rRect: RRect): DoubleArray {
        val vector = DoubleArray(16 * 3)
        var index = 0

        val images = scaleImage(byteBufferImage, rRect, SCALED_SIZE, SCALED_SIZE)
        debugCallback(images)

        for (image in images) {
            val dct = DoubleDCT_2D(SCALED_SIZE.toLong(), SCALED_SIZE.toLong())
            dct.forward(image.buffer, true)

            for (x in 0 until 4) {
                for (y in 0 until 4) {
                    vector.set(index++, image.getPixel(x, y))
                }
            }
        }

        return vector
    }

    fun scaleImage(inImage: ByteBufferImage, rRect: RRect, outW: Int, outH: Int): Array<ATImage> {
        val scaleX = (inImage.w * rRect.w) / outW
        val scaleY = (inImage.h * rRect.h) / outH
        val images = Array(3) {
            ATImage(outW, outH, kotlin.DoubleArray(outW * outH))
        }
        inImage.buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (outX in 0 until outW) {
            for (outY in 0 until outH) {
                val inX = scaleX * outX + rRect.l * inImage.w
                val inY = scaleY * outY + rRect.t * inImage.h

                val X0 = inX.toInt()
                val Y0 = inY.toInt()
                val X1 = inX.toInt() + 1
                val Y1 = inY.toInt() + 1

                val X0Y0 = inImage.buffer.getInt(X0 + Y0 * inImage.stride)
                val X1Y0 = inImage.buffer.getInt(X1 + Y0 * inImage.stride)
                val X0Y1 = inImage.buffer.getInt(X0 + Y1 * inImage.stride)
                val X1Y1 = inImage.buffer.getInt(X1 + Y1 * inImage.stride)

                for (i in 0 until 2) {
                    val shift = 8 * i
                    /*
                     * bilinear filtering
                     */
                    val v0 = (inX - X0) * X0Y0.shr(shift).and(0xff) + (X1 - inX) * X1Y0.shr(shift).and(0xff)
                    val v1 = (inX - X0) * X0Y1.shr(shift).and(0xff) + (X1 - inX) * X1Y1.shr(shift).and(0xff)
                    val v = ((inY - Y0) * v0 + (Y1 - inY) * v1) / 255.0

                    images[i].buffer.set(outY * outW + outX, v)

                    //images[i].buffer.set(outY * outW + outX, outX.toDouble() / outW)
                }
            }
        }

        return images
    }

    companion object {
        const val SCALED_SIZE = 32
    }
}


