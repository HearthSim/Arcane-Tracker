package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import net.mbonnin.arcanetracker.detector.ByteBufferImage
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCapture constructor(mediaProjection: MediaProjection) : ImageReader.OnImageAvailableListener {
    internal var mCallback: MediaProjection.Callback = object : MediaProjection.Callback() {
    }
    internal var mWidth: Int = 0
    internal var mHeight: Int = 0
    internal var mImageReader: ImageReader
    internal var mHandler = Handler()
    private val imageConsumerList = CopyOnWriteArrayList<Consumer>()

    interface Consumer {
        fun accept(bbImage: ByteBufferImage)
    }
    fun addImageConsumer(consumer: Consumer) {
        imageConsumerList.add(consumer)
    }

    fun removeImageConsumer(consumer: Consumer) {
        imageConsumerList.remove(consumer)
    }

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireLatestImage()
        if (image != null /*Handler*/) {
            if (image.planes.size != 1) {
                Timber.d("unknown image with %d planes", image.planes.size)
                image.close()
                return
            }

            val bbImage = ByteBufferImage(image.width, image.height, image.planes[0].buffer, image.planes[0].rowStride)

            for (bbImageConsumer in imageConsumerList) {
                bbImageConsumer.accept(bbImage)
            }

            image.close()
        }
    }

    private val virtualDisplay: VirtualDisplay

    init {
        mediaProjection.registerCallback(mCallback, null)

        val wm = HDTApplication.get().getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val display = wm.defaultDisplay
        val point = Point()
        display.getRealSize(point)

        // if we start in landscape, we might have the wrong orientation
        mWidth = if (point.x > point.y) point.x else point.y
        mHeight = if (point.y < point.x) point.y else point.x

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 3)

        val worker = ScreenCaptureWorker()
        worker.start()
        val handler = worker.waitUntilReady()
        mImageReader.setOnImageAvailableListener(this, handler)
        virtualDisplay = mediaProjection.createVirtualDisplay("Hearthstone Deck Tracker",
                mWidth, mHeight, 320,
                0,
                mImageReader.surface, null, null)/*Callbacks*/
    }

    fun release() {
        virtualDisplay.release()
    }
    /*
     * the thread where the image processing is made. Maybe we could have reused the ImageReader looper
     */
    internal class ScreenCaptureWorker : HandlerThread("ScreenCaptureWorker") {

        fun waitUntilReady(): Handler {
            return Handler(looper)
        }
    }
}

