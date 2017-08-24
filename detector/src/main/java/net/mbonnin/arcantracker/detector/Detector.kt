package net.mbonnin.arcantracker.detector

import org.jtransforms.dct.DoubleDCT_2D
import java.nio.ByteBuffer

class ByteBufferImage(val w: Int, val h: Int, val buffer: ByteBuffer, val stride: Int) {
    fun getPixel(x: Int, y: Int): Byte {
        return buffer.get(x + y * stride);
    }
}

class DoubleArrayImage(val w: Int, val h: Int, val buffer: DoubleArray) {
    fun getPixel(x: Int, y: Int): Double {
        return buffer.get(x + y * w);
    }
}


class Detector {
    val SCALED_SIZE = 32

    fun detectRank(byteBufferImages: Array<ByteBufferImage>) {
        val vector = getRankVectorDCT(byteBufferImages)
        val minDistance = Double.MAX_VALUE
    }

    fun getRankVectorDCT(byteBufferImages: Array<ByteBufferImage>): DoubleArray {
        val vector = DoubleArray(16 * 3)
        var index = 0;

        for (i in 0..2) {
            val image = scaleImage(byteBufferImages.get(i), SCALED_SIZE, SCALED_SIZE)

            val dct = DoubleDCT_2D(SCALED_SIZE.toLong(), SCALED_SIZE.toLong())
            dct.forward(image.buffer, true)

            for (x in 0..3) {
                for (y in 0..3) {
                    vector.set(index++, image.getPixel(x, y))
                }
            }
        }

        return vector;
    }

    fun scaleImage(inImage: ByteBufferImage, outW: Int, outH: Int): DoubleArrayImage {
        val outImage = DoubleArrayImage(outW, outH, kotlin.DoubleArray(outW * outH));
        val scaleX = (inImage.w as Double) / outW;
        val scaleY = (inImage.h as Double) / outH;

        for (outX in 0..outW - 1) {
            for (outY in 0..outH - 1) {
                val inX = scaleX * outX;
                val inY = scaleY * outY;

                /*
                 * bilinear filtering
                 */
                val v1 = (inX - inX.toInt()) * inImage.getPixel(inX.toInt(), inY.toInt()) + (inX.toInt() + 1 - inX) * inImage.getPixel(inX.toInt() + 1, inY.toInt());
                val v2 = (inX - inX.toInt()) * inImage.getPixel(inX.toInt(), inY.toInt() + 1) + (inX.toInt() + 1 - inX) * inImage.getPixel(inX.toInt() + 1, inY.toInt() + 1);

                outImage.buffer.set(outY * outW + outX, (inY - inY.toInt()) * v1 + (inY.toInt() + 1 - inY) * v2);
            }
        }

        return outImage;
    }
}


