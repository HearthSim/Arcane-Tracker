package net.mbonnin.arcanetracker.parser;

/**
 * Created by martin on 11/21/16.
 */

public class Play {

    public Play() {};

    public Play(int turn, boolean isOpponent, String cardId) {
        this.turn = turn;
        this.isOpponent = isOpponent;
        this.cardId = cardId;
    }

    public int turn;
    public boolean isOpponent;
    public String cardId;
}
