package net.mbonnin.arcanetracker;

import java.util.ArrayList;
import java.util.UUID;

import io.paperdb.Paper;

/**
 * Created by martin on 10/20/16.
 */

public class DeckList {
    public static final String ARENA_DECK_ID = "ARENA_DECK_ID";
    private static ArrayList<Deck> sList;
    static final String KEY_LIST = "list";
    static final String KEY_ARENA_DECK = "arena_deck";
    private static Deck sPlayerGameDeck;
    private static Deck sArenaDeck;
    private static Deck sOpponentGameDeck;

    public static Deck createDeck(int classIndex) {
        Deck deck = new Deck();
        deck.classIndex = classIndex;
        deck.id = UUID.randomUUID().toString();
        deck.name = "Your Deck";

        get().add(deck);

        save();
        return deck;
    }

    public static void deleteDeck(Deck deck) {
        sList.remove(deck);
        save();
    }
    static ArrayList<Deck> get() {
        if (sList == null) {
            sList = Paper.book().read(KEY_LIST);
        }
        if (sList == null) {
            sList = new ArrayList<>();
            save();
        }

        return sList;
    }

    public static void save() {
        Paper.book().write(KEY_LIST, sList);
    }
    public static void saveArena() {
        Paper.book().write(KEY_ARENA_DECK, getArenaDeck());
    }

    public static Deck getPlayerGameDeck() {
        if (sPlayerGameDeck == null) {
            sPlayerGameDeck = new Deck();
        }
        return sPlayerGameDeck;
    }

    public static Deck getArenaDeck() {
        if (sArenaDeck == null) {
            sArenaDeck = Paper.book().read(KEY_ARENA_DECK);
            if (sArenaDeck == null) {
                sArenaDeck = new Deck();
                sArenaDeck.id = ARENA_DECK_ID;
                sArenaDeck.name = "Arena Deck";
            }
        }
        return sArenaDeck;
    }

    public static Deck getOpponentGameDeck() {
        if (sOpponentGameDeck == null) {
            sOpponentGameDeck = new Deck();
            sOpponentGameDeck.name = "Opponent's Deck";
        }
        return sOpponentGameDeck;
    }
}
