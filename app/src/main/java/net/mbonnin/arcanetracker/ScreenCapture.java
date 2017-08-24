package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.view.Display;
import android.view.WindowManager;

import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcantracker.detector.ByteBufferImage;
import net.mbonnin.arcantracker.detector.Detector;

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

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            ByteBufferImage byteBufferImages[] = new ByteBufferImage[3];
            for (int i = 0; i < 3; i++) {
                byteBufferImages[i] = new ByteBufferImage(image.getWidth(), image.getHeight(), image.getPlanes()[i].getBuffer(), image.getPlanes()[i].getRowStride());
            }

            if ("TOURNAMENT".equals(LoadingScreenParser.get().getMode())) {
                mDetector.detectRank(byteBufferImages);
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
        mWidth = display.getWidth();
        mHeight = display.getHeight();

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

    static class ScreenCaptureWorker extends HandlerThread {
        public ScreenCaptureWorker() {
            super("ScreenCaptureWorker");
        }
        public Handler waitUntilReady() {
            return new Handler(getLooper());
        }
    }
}

