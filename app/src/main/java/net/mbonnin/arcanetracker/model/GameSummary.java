package net.mbonnin.arcanetracker.model;

public class GameSummary {
    public String deckName;
    public int hero;
    public int opponentHero;
    public boolean coin;
    public boolean win;
    public String date;

    public String hsreplayUrl;
    // added in v213
    public int bnetGameType;
}
