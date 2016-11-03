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

    private static int STATE_NOTHING = 0;
    private static int STATE_WAS_OPEN = 1;
    private static int STATE_WAS_CLOSED = 2;

    private int state = STATE_NOTHING;

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

            View view = MainViewCompanion.get().getMainView();
            view.getLocationOnScreen(mPosition);

            //Timber.i("view %d - %d", mPosition[1], view.getHeight());
            if (mPosition[1] != 0) {
                if (Settings.get(Settings.AUTO_QUIT, true)) {
                    if (state == STATE_NOTHING) {
                        Timber.i("Exit FullScreen detected");
                        state = MainViewCompanion.get().isOpen() ? STATE_WAS_OPEN: STATE_WAS_CLOSED;
                        MainViewCompanion.get().setOpen(false);
                        ViewManager.get().removeAllViewsExcept(view);
                    }
                }
            } else {
                if (state != STATE_NOTHING) {
                    Timber.i("Back to FullScreen");
                    MainViewCompanion.get().setOpen(state == STATE_WAS_OPEN);
                    MainViewCompanion.get().show(true);
                    state = STATE_NOTHING;
                }
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    public void start() {
        mHandler.postDelayed(mCheckFullScreenRunnable, 4000);
    }

    public void stop() {
        mHandler.removeCallbacks(mCheckFullScreenRunnable);
    }

    public QuitDetector() {
        mHandler = new Handler();
    }
}
