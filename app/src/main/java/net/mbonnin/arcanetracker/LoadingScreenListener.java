package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.LoadingScreenParser;

/**
 * Created by martin on 11/7/16.
 */
public class LoadingScreenListener implements LoadingScreenParser.Listener {
    static LoadingScreenListener sLoadingScreenListener;

    public static LoadingScreenListener get() {
        if (sLoadingScreenListener == null) {
            sLoadingScreenListener = new LoadingScreenListener();

        }
        return sLoadingScreenListener;
    }
    private int mMode;

    public int getMode() {
        return mMode;
    }

    @Override
    public void modeChanged(int newMode) {
        mMode = newMode;

    }
}
