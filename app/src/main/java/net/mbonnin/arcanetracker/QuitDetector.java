package net.mbonnin.arcanetracker;

import android.os.Handler;
import android.view.View;

import timber.log.Timber;

/**
 * Created by martin on 11/2/16.
 */

public class QuitDetector {
    private Handler mHandler;
    private int[] mPosition = new int[2];
    public StopServiceRunnable mStopServiceRunnable;
    View mView;
    ViewManager.Params mParams;

    static QuitDetector sQuitDetector;

    public static QuitDetector get() {
        if (sQuitDetector == null) {
            sQuitDetector = new QuitDetector();
        }
        return sQuitDetector;
    }

    private final Runnable mCheckFullScreenRunnable = new Runnable() {

        @Override
        public void run() {
            mView.getLocationOnScreen(mPosition);

            if (mPosition[1] != 0) {
                if (Settings.get(Settings.AUTO_QUIT, true)) {
                    if (mStopServiceRunnable == null) {
                        mStopServiceRunnable = new StopServiceRunnable();
                        Timber.i("Exit FullScreen detected");
                        MainViewCompanion.get().show(false);

                        //mHandler.postDelayed(mStopServiceRunnable, 10000);
                    }
                }
            } else {
                if (mStopServiceRunnable != null) {
                    Timber.i("Back to FullScreen");
                    MainViewCompanion.get().show(true);
                    mHandler.removeCallbacks(mStopServiceRunnable);
                    mStopServiceRunnable = null;
                }
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    private class StopServiceRunnable implements Runnable {
        @Override
        public void run() {
            Timber.i("Out of fullscreen for some time, stop service");
            MainService.stop();
            mHandler.removeCallbacks(mCheckFullScreenRunnable);
            mStopServiceRunnable = null;
        }
    }


    public void start() {
        mParams = new ViewManager.Params();
        mView = new View(ArcaneTrackerApplication.getContext());
        ViewManager.get().addView(mView, mParams);

        mHandler.postDelayed(mCheckFullScreenRunnable, 4000);
    }

    public void stop() {
        if (mStopServiceRunnable != null) {
            mHandler.removeCallbacks(mStopServiceRunnable);
            mStopServiceRunnable = null;
        }

        mHandler.removeCallbacks(mCheckFullScreenRunnable);
    }

    public QuitDetector() {
        mHandler = new Handler();
    }
}
