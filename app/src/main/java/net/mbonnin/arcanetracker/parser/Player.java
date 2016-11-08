package net.mbonnin.arcanetracker.parser;

import java.util.ArrayList;

/**
 * Created by martin on 11/7/16.
 */
public class Player {
    public PlayerEntity entity;
    public String battleTag;
    public boolean isOpponent;
    public boolean hasCoin;

    public CardEntity hero;
    public CardEntity heroPower;

    public ArrayList<CardEntity> hand = new ArrayList<>();
    public ArrayList<CardEntity> deck = new ArrayList<>();
    public ArrayList<CardEntity> graveyard = new ArrayList<>();
    public ArrayList<CardEntity> secret = new ArrayList<>();

    public void reset() {
        hand.clear();
        deck.clear();
        graveyard.clear();
        secret.clear();
    }
}
