package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.view.Display;
import android.view.WindowManager;

import net.mbonnin.arcanetracker.detector.ByteBufferImage;
import net.mbonnin.arcanetracker.detector.Detector;
import net.mbonnin.arcanetracker.detector.DetectorKt;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;

import timber.log.Timber;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenCapture implements ImageReader.OnImageAvailableListener{
    private final Detector mDetector;

    MediaProjection mediaProjection;
    MediaProjection.Callback mCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
        }
    };
    int mWidth;
    int mHeight;
    ImageReader mImageReader;
    Handler mHandler = new Handler();

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            if (image.getPlanes().length != 1) {
                Timber.d("unknown image with %d planes", image.getPlanes().length);
                image.close();
                return;
            }

            if ("TOURNAMENT".equals(LoadingScreenParser.get().getMode())) {
                ByteBufferImage bbImage = new ByteBufferImage(image.getWidth(), image.getHeight(), image.getPlanes()[0].getBuffer(), image.getPlanes()[0].getRowStride());
                int format = mDetector.detectFormat(bbImage);
                if (format != DetectorKt.FORMAT_UNKNOWN) {
                    ScreenCaptureResult.setFormat(format);
                }
                int mode = mDetector.detectMode(bbImage);
                if (mode != DetectorKt.MODE_UNKNOWN) {
                    ScreenCaptureResult.setMode(mode);
                    if (mode == DetectorKt.MODE_RANKED) {
                        int rank = mDetector.detectRank(bbImage);
                        ScreenCaptureResult.setRank(rank);
                    }
                }
            }
            image.close();
        }
    }

    public ScreenCapture(Context context, MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        mediaProjection.registerCallback(mCallback, null);

        mDetector = new Detector();

        WindowManager wm = (android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);
        mWidth = point.x;
        mHeight = point.y;

        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 3);

        ScreenCaptureWorker worker = new ScreenCaptureWorker();
        worker.start();
        Handler handler = worker.waitUntilReady();
        mImageReader.setOnImageAvailableListener(this, handler);
        mediaProjection.createVirtualDisplay("Arcane Tracker",
                mWidth, mHeight, 320,
                0,
                mImageReader.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    /*
     * the thread where the image processing is made. Maybe we could have reused the ImageReader looper
     */
    static class ScreenCaptureWorker extends HandlerThread {
        public ScreenCaptureWorker() {
            super("ScreenCaptureWorker");
        }
        public Handler waitUntilReady() {
            return new Handler(getLooper());
        }
    }
}

