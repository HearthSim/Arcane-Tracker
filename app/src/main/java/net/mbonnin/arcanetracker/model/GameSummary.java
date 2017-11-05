package net.mbonnin.arcanetracker.model;

import net.mbonnin.arcanetracker.BnetGameType;

public class GameSummary {
    public String deckName;
    public int hero;
    public int opponentHero;
    public boolean coin;
    public boolean win;
    public String date;

    public String hsreplayUrl;
    // added in v213
    public BnetGameType bnetGameType;
}
