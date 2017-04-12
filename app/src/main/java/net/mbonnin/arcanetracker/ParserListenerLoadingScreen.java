package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.LoadingScreenParser;

/**
 * Created by martin on 11/7/16.
 */
public class ParserListenerLoadingScreen implements LoadingScreenParser.Listener {

    private final MainViewCompanion mainViewCompanion;
    private final DeckListManager deckListManager;
    private int mMode;

    public ParserListenerLoadingScreen(MainViewCompanion mainViewCompanion,DeckListManager deckListManager) {
        this.mainViewCompanion = mainViewCompanion;
        this.deckListManager = deckListManager;
    }


    public int getMode() {
        return mMode;
    }

    @Override
    public void modeChanged(int newMode) {
        mMode = newMode;
        if (newMode == LoadingScreenParser.MODE_ARENA) {
            mainViewCompanion.getPlayerCompanion().setDeck(deckListManager.getArenaDeck(), null);
        }
    }
}
