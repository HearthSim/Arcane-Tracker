package net.mbonnin.arcanetracker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.paperdb.Book;
import io.paperdb.Paper;

/**
 * Created by martin on 10/20/16.
 */

public class DeckListManager {
    public static final String ARENA_DECK_ID = "ARENA_DECK_ID";
    private List<Deck> sList;
    private static final String KEY_LIST = "list";
    private static final String KEY_ARENA_DECK = "arena_deck";
    private Deck sArenaDeck;
    private Deck sOpponentDeck;
    private final CardDb cardDb;
    private final Book book;
    private final String deckName;
    private final String opponentsDeckName;

    public DeckListManager(CardDb cardDb, Book book, String yourDeckName, String opponentsDeckName) {
        this.cardDb = cardDb;
        this.book = book;
        this.deckName = yourDeckName;
        this.opponentsDeckName = opponentsDeckName;
    }

    public Deck createDeck(int classIndex) {
        Deck deck = new Deck();
        deck.classIndex = classIndex;
        deck.id = UUID.randomUUID().toString();
        deck.name = deckName;

        get().add(deck);

        save();
        return deck;
    }

    public void deleteDeck(Deck deck) {
        sList.remove(deck);
        save();
    }

    List<Deck> get() {
        if (sList == null) {
            sList = book.read(KEY_LIST);
        }
        if (sList == null) {
            sList = new ArrayList<>();
            save();
        }

        return sList;
    }

    public void save() {
        book.write(KEY_LIST, sList);
    }
    public void saveArena() {
        book.write(KEY_ARENA_DECK, getArenaDeck());
    }

    public Deck getArenaDeck() {
        if (sArenaDeck == null) {
            sArenaDeck = book.read(KEY_ARENA_DECK);
            if (sArenaDeck == null) {
                sArenaDeck = new Deck();
                sArenaDeck.id = ARENA_DECK_ID;
                sArenaDeck.name = opponentsDeckName;
            }
        }
        cardDb.checkClassIndex(sArenaDeck);
        return sArenaDeck;
    }

    public Deck getOpponentDeck() {
        if (sOpponentDeck == null) {
            sOpponentDeck = new Deck();
            sOpponentDeck.name = opponentsDeckName;
        }
        return sOpponentDeck;
    }
}
