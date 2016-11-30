package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.LoadingScreenParser;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */
public class ParserListenerLoadingScreen implements LoadingScreenParser.Listener {
    static ParserListenerLoadingScreen sParserListenerLoadingScreen;

    public static ParserListenerLoadingScreen get() {
        if (sParserListenerLoadingScreen == null) {
            sParserListenerLoadingScreen = new ParserListenerLoadingScreen();

        }
        return sParserListenerLoadingScreen;
    }
    private int mMode;

    public int getMode() {
        return mMode;
    }

    @Override
    public void modeChanged(int newMode) {
        mMode = newMode;
        Timber.d("mode " + LoadingScreenParser.friendlyMode(newMode));
        if (newMode == LoadingScreenParser.MODE_ARENA) {
            MainViewCompanion.getPlayerCompanion().setDeck(DeckList.getArenaDeck());
        }
    }
}
