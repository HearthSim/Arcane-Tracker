package net.mbonnin.arcantracker.detector

import ar.com.hjg.pngj.ImageInfo
import ar.com.hjg.pngj.ImageLineInt
import ar.com.hjg.pngj.PngReader
import ar.com.hjg.pngj.PngWriter
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer


fun pngToByteBufferImage(inputStream: InputStream): ByteBufferImage {
    val reader = PngReader(inputStream)
    val size = reader.imgInfo.rows * reader.imgInfo.cols * 4;
    val byteBuffer = ByteBuffer.allocate(size);

    val channels = reader.imgInfo.channels;

    System.out.println("size: " + reader.imgInfo.cols + "x" + reader.imgInfo.rows)
    System.out.println("channels: " + channels)
    System.out.println("bitdepth: " + reader.imgInfo.bitDepth)

    byteBuffer.position(0)
    for (row in 0 until reader.imgInfo.rows) {
        val l1 = reader.readRow() as ImageLineInt
        val scanline = l1.scanline;
        for (i in 0 until reader.imgInfo.cols) {
            byteBuffer.put(scanline[i * channels + 0].toByte())
            byteBuffer.put(scanline[i * channels + 1].toByte())
            byteBuffer.put(scanline[i * channels + 2].toByte())
            byteBuffer.put(255.toByte())
        }
    }
    reader.end()

    return ByteBufferImage(reader.imgInfo.cols, reader.imgInfo.rows, byteBuffer, reader.imgInfo.cols * 4)
}

fun byteBufferImageToPng(byteBufferImage: ByteBufferImage, path: String) {

    val outputStream = FileOutputStream(path)
    val imgInfo = ImageInfo(byteBufferImage.w, byteBufferImage.h, 8, false)
    val writer = PngWriter(outputStream, imgInfo)
    val imageLine = ImageLineInt(imgInfo)
    val channels = imgInfo.channels

    byteBufferImage.buffer.position(0)
    for (row in 0 until imgInfo.rows) {
        val scanline = imageLine.scanline;
        byteBufferImage.buffer.position(row * byteBufferImage.stride);
        for (i in 0 until imgInfo.cols) {
            scanline[i * channels + 0] =  byteBufferImage.buffer.get().toInt()
            scanline[i * channels + 1] =  byteBufferImage.buffer.get().toInt()
            scanline[i * channels + 2] =  byteBufferImage.buffer.get().toInt()
            byteBufferImage.buffer.get()
        }

        writer.writeRow(imageLine)
    }
    writer.end()
}

fun atImageToPng(atImage: ATImage, path: String) {

    val outputStream = FileOutputStream(path)
    val imgInfo = ImageInfo(atImage.w, atImage.h, 8, false)
    val writer = PngWriter(outputStream, imgInfo)
    val imageLine = ImageLineInt(imgInfo)
    val channels = imgInfo.channels

    for (row in 0 until imgInfo.rows) {
        val scanline = imageLine.scanline;
        for (i in 0 until imgInfo.cols) {
            scanline[i * channels + 0] =  (255 * atImage.buffer.get(i + row * atImage.w)).toInt()
            scanline[i * channels + 1] =  (255 * atImage.buffer.get(i + row * atImage.w)).toInt()
            scanline[i * channels + 2] =  (255 * atImage.buffer.get(i + row * atImage.w)).toInt()
        }

        writer.writeRow(imageLine)
    }
    writer.end()
}