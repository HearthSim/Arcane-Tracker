package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;

/**
 * Created by martin on 11/7/16.
 */
public class Player {

    public interface Listener {
        void onPlayerStateChanged();
    }

    public PlayerEntity entity;
    public String battleTag;
    public boolean isOpponent;
    public boolean hasCoin;

    public CardEntity hero;
    public CardEntity heroPower;

    public ArrayList<CardEntity> cards = new ArrayList<>();

    public void reset() {
        cards.clear();
    }

    public ArrayList<Listener> listeners = new ArrayList<>();

}
