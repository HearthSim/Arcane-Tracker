package net.mbonnin.arcanetracker

import android.annotation.TargetApi
import android.content.Context
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.view.WindowManager
import timber.log.Timber


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenCapture {

    private var mProjection: MediaProjection

    private val mCallback = object: MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
        }
    }

    private var mHeight: Int
    private var mWidth: Int

    private var mImageReader: ImageReader

    private val mImageAvailableListener = object: ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            val image = reader?.acquireLatestImage()
            if (image != null) {
                Timber.d("image: %d", image.format)
                image.close()
            }
        }

    }

    constructor(context: Context, projection: MediaProjection) {
        mProjection = projection
        mProjection.registerCallback(mCallback, null)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        mWidth = display.width;
        mHeight = display.height

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 3)

        mImageReader.setOnImageAvailableListener(mImageAvailableListener, Handler())
        mProjection.createVirtualDisplay("ScreenSharingDemo",
                mWidth, mHeight, 320,
                0,
                mImageReader.surface, null /*Callbacks*/, null /*Handler*/)
    }
}