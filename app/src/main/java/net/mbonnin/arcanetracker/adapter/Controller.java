package net.mbonnin.arcanetracker.adapter;

import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.parser.Player;

/**
 * Created by martin on 11/21/16.
 */
public abstract class Controller implements Player.Listener, Deck.Listener {
    protected Player mPlayer;
    protected Deck mDeck;

    @Override
    public void onPlayerStateChanged() {
        update();
    }


    public void setDeck(Deck deck, Player player) {
        deck.setListener(this);

        mDeck = deck;
        if (mPlayer != null) {
            mPlayer.listeners.remove(this);
        }
        mPlayer = player;
        if (mPlayer != null) {
            mPlayer.registerListener(this);
        }
        update();
    }

    @Override
    public void onDeckChanged() {
        update();
    }

    abstract protected void update();

}
