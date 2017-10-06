package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresApi
import net.mbonnin.arcanetracker.detector.*
import net.mbonnin.arcanetracker.parser.LoadingScreenParser
import rx.Single
import rx.SingleSubscriber
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCapture private constructor(internal var mediaProjection: MediaProjection) : ImageReader.OnImageAvailableListener {
    private val mDetector: Detector
    internal var mCallback: MediaProjection.Callback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
        }
    }
    internal var mWidth: Int = 0
    internal var mHeight: Int = 0
    internal var mImageReader: ImageReader
    internal var mHandler = Handler()
    private val mSubscriberList = LinkedList<SingleSubscriber<in File>>()

    override fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireLatestImage()
        if (image != null /*Handler*/) {
            if (image.planes.size != 1) {
                Timber.d("unknown image with %d planes", image.planes.size)
                image.close()
                return
            }

            val bbImage = ByteBufferImage(image.width, image.height, image.planes[0].buffer, image.planes[0].rowStride)

            var subscriber: SingleSubscriber<in File>? = null
            synchronized(mSubscriberList) {
                if (!mSubscriberList.isEmpty()) {
                    subscriber = mSubscriberList.removeFirst()
                }
            }

            if (subscriber != null) {
                val file = File(ArcaneTrackerApplication.get().getExternalFilesDir(null), "screenshot.jpg")
                val bitmap = Bitmap.createBitmap(bbImage.w, bbImage.h, Bitmap.Config.ARGB_8888)
                val buffer = bbImage.buffer
                val stride = bbImage.stride
                for (j in 0 until bbImage.h) {
                    for (i in 0 until bbImage.w) {
                        bitmap.setPixel(i, j, Color.argb(255,
                                buffer.get(i * 4 + j * stride).toInt(),
                                buffer.get(i * 4 + 1 + j * stride).toInt(),
                                buffer.get(i * 4 + 2 + j * stride).toInt()))
                    }
                }
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(file))
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }

                subscriber!!.onSuccess(file)
            }

            if (LoadingScreenParser.MODE_TOURNAMENT == LoadingScreenParser.get().mode) {
                val format = mDetector.detectFormat(bbImage)
                if (format != FORMAT_UNKNOWN) {
                    ScreenCaptureResult.setFormat(format)
                }
                val mode = mDetector.detectMode(bbImage)
                if (mode != MODE_UNKNOWN) {
                    ScreenCaptureResult.setMode(mode)
                    if (mode == MODE_RANKED) {
                        val rank = mDetector.detectRank(bbImage)
                        if (rank != RANK_UNKNOWN) {
                            ScreenCaptureResult.setRank(rank)
                        }
                    }
                }
            }

            if (LoadingScreenParser.MODE_DRAFT == LoadingScreenParser.get().mode) {
                val hero = getPlayerClass(DeckList.getArenaDeck().classIndex)

                val arenaResult = mDetector.detectArena(bbImage, hero)
                ScreenCaptureResult.setArena(arenaResult, hero)
            } else {
                ScreenCaptureResult.clearArena()
            }
            image.close()
        }
    }

    init {
        mediaProjection.registerCallback(mCallback, null)

        mDetector = Detector(ArcaneTrackerApplication.get(), ArcaneTrackerApplication.get().hasTabletLayout())

        val wm = ArcaneTrackerApplication.get().getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
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
        mediaProjection.createVirtualDisplay("Arcane Tracker",
                mWidth, mHeight, 320,
                0,
                mImageReader.surface, null, null)/*Callbacks*/
    }

    fun screenShotSingle(): Single<File> {
        return Single.create { singleSubscriber ->
            synchronized(mSubscriberList) {
                mSubscriberList.add(singleSubscriber)
            }
        }
    }

    /*
     * the thread where the image processing is made. Maybe we could have reused the ImageReader looper
     */
    internal class ScreenCaptureWorker : HandlerThread("ScreenCaptureWorker") {

        fun waitUntilReady(): Handler {
            return Handler(looper)
        }
    }

    companion object {
        private lateinit var sScreenCapture: ScreenCapture

        fun get(): ScreenCapture? {
            return sScreenCapture
        }

        fun create(mediaProjection: MediaProjection): ScreenCapture {
            sScreenCapture = ScreenCapture(mediaProjection)
            return sScreenCapture
        }
    }

}

