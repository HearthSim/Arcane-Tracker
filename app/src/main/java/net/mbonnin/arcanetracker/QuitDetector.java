package net.mbonnin.arcanetracker;

import android.os.Handler;
import android.view.View;

import timber.log.Timber;

/**
 * Created by martin on 11/2/16.
 */

public class QuitDetector {
    private final MainViewCompanion mainViewCompanion;
    private final ViewManager viewManager;
    private final Settings settings;
    private Handler mHandler;
    private int[] mPosition = new int[2];

    private static int STATE_NOTHING = 0;
    private static int STATE_WAS_OPEN = 1;
    private static int STATE_WAS_CLOSED = 2;

    private int state = STATE_NOTHING;

    private final Runnable mCheckFullScreenRunnable = new Runnable() {

        @Override
        public void run() {

            View view = mainViewCompanion.getMainView();
            view.getLocationOnScreen(mPosition);

            //Timber.i("view %d - %d", mPosition[1], view.getHeight());
            if (mPosition[1] != 0) {
                if (settings.get(Settings.AUTO_QUIT, true)) {
                    if (state == STATE_NOTHING) {
                        Timber.i("Exit FullScreen detected");
                        state = mainViewCompanion.isOpen() ? STATE_WAS_OPEN: STATE_WAS_CLOSED;
                        mainViewCompanion.setOpen(false);
                        viewManager.removeAllViewsExcept(view);
                    }
                }
            } else {
                if (state != STATE_NOTHING) {
                    Timber.i("Back to FullScreen");
                    mainViewCompanion.setOpen(state == STATE_WAS_OPEN);
                    mainViewCompanion.show(true);
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

    public QuitDetector(MainViewCompanion mainViewCompanion, ViewManager viewManager, Settings settings) {
        this.mainViewCompanion = mainViewCompanion;
        this.viewManager = viewManager;
        this.settings = settings;
        this.mHandler = new Handler();
    }
}
