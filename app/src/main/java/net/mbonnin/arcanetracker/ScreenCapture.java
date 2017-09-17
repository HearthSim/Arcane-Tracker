package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import rx.Single;
import rx.SingleSubscriber;
import timber.log.Timber;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class ScreenCapture implements ImageReader.OnImageAvailableListener{
    private static ScreenCapture sScreenCapture;
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
    private LinkedList<SingleSubscriber<? super File>> mSubscriberList = new LinkedList<>();

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            if (image.getPlanes().length != 1) {
                Timber.d("unknown image with %d planes", image.getPlanes().length);
                image.close();
                return;
            }

            ByteBufferImage bbImage = new ByteBufferImage(image.getWidth(), image.getHeight(), image.getPlanes()[0].getBuffer(), image.getPlanes()[0].getRowStride());

            SingleSubscriber<? super File> subscriber = null;
            synchronized (mSubscriberList) {
                if (!mSubscriberList.isEmpty()) {
                    subscriber = mSubscriberList.removeFirst();
                }
            }

            if (subscriber != null) {
                File file = new File(ArcaneTrackerApplication.get().getExternalFilesDir(null), "screenshot.jpg");
                Timber.d("screen capture1");
                Bitmap bitmap = Bitmap.createBitmap(bbImage.getW(), bbImage.getH(), Bitmap.Config.ARGB_8888);
                ByteBuffer buffer = bbImage.getBuffer();
                int stride = bbImage.getStride();
                for (int j = 0; j < bbImage.getH(); j++) {
                    for (int i = 0; i < bbImage.getW(); i++) {
                        bitmap.setPixel(i, j, Color.argb(255, buffer.get(i * 4 + j * stride) & 0xff, buffer.get(i * 4 + 1 + j * stride) & 0xff, buffer.get(i * 4 + 2 + j * stride) & 0xff));
                    }
                }
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(file));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Timber.d("screen capture2");
                subscriber.onSuccess(file);
            }

            if ("TOURNAMENT".equals(LoadingScreenParser.get().getMode())) {
                int format = mDetector.detectFormat(bbImage);
                if (format != DetectorKt.FORMAT_UNKNOWN) {
                    ScreenCaptureResult.setFormat(format);
                }
                int mode = mDetector.detectMode(bbImage);
                if (mode != DetectorKt.MODE_UNKNOWN) {
                    ScreenCaptureResult.setMode(mode);
                    if (mode == DetectorKt.MODE_RANKED) {
                        int rank = mDetector.detectRank(bbImage);
                        if (rank != DetectorKt.RANK_UNKNOWN) {
                            ScreenCaptureResult.setRank(rank);
                        }
                    }
                }
            }
            image.close();
        }
    }

    private ScreenCapture( MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        mediaProjection.registerCallback(mCallback, null);

        mDetector = new Detector(ArcaneTrackerApplication.get().hasTabletLayout());

        WindowManager wm = (android.view.WindowManager)ArcaneTrackerApplication.get().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point point = new Point();
        display.getRealSize(point);

        // if we start in landscape, we might have the wrong orientation
        mWidth = point.x > point.y ? point.x : point.y;
        mHeight = point.y < point.x ? point.y : point.x;

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

    public static ScreenCapture get() {
        return sScreenCapture;
    }

    public static ScreenCapture create(MediaProjection mediaProjection) {
        sScreenCapture = new ScreenCapture(mediaProjection);
        return sScreenCapture;
    }

    public Single<File> screenShotSingle() {
        return Single.create(singleSubscriber -> {
            synchronized (mSubscriberList) {
                mSubscriberList.add(singleSubscriber);
            }
        });
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

