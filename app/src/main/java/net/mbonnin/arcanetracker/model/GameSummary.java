package net.mbonnin.arcanetracker.model;

import net.mbonnin.arcanetracker.PaperDb;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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

    static final String KEY_GAME_LIST = "KEY_GAME_LIST";
    static List<GameSummary> savedGameSummaryList;

    static {
        savedGameSummaryList = PaperDb.INSTANCE.read(KEY_GAME_LIST);
        if (savedGameSummaryList == null) {
            savedGameSummaryList = new ArrayList();
        }
    }

    static public void eraseGameSummary() {
        savedGameSummaryList.clear();
        sync();
    }

    public static void addFirst(@NotNull GameSummary summary) {
        savedGameSummaryList.add(0, summary);
        sync();
    }

    public static void sync() {
        PaperDb.INSTANCE.write(KEY_GAME_LIST, savedGameSummaryList);
    }

    public static List<GameSummary> getGameSummary() {
        return savedGameSummaryList;
    }
}
