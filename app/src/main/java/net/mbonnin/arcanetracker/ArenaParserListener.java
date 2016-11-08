package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.ArenaParser;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ArenaParserListener implements ArenaParser.Listener {
    static ArenaParserListener sArenaParserListener;
    public static ArenaParserListener get() {
        if (sArenaParserListener == null) {
            sArenaParserListener = new ArenaParserListener();
        }
        return sArenaParserListener;
    }

    @Override
    public void clear() {
        Deck deck = DeckList.getArenaDeck();
        deck.clear();
        DeckList.saveArena();
    }

    @Override
    public void heroDetected(int classIndex) {
        Deck deck = DeckList.getArenaDeck();
        deck.classIndex = classIndex;
        MainViewCompanion.getPlayerCompanion().setDeck(deck);

    }

    @Override
    public void addCard(String cardId) {
        Deck deck = DeckList.getArenaDeck();
        deck.addCard(cardId, 1);
        DeckList.saveArena();
    }

    @Override
    public void addIfNotAlreadyThere(String cardId) {
        Deck deck = DeckList.getArenaDeck();
        Card card = ArcaneTrackerApplication.getCard(cardId);
        if (!deck.cards.containsKey(cardId)) {
            if (card.collectible) {
                deck.addCard(cardId, 1);
                DeckList.saveArena();
            } else {
                Timber.e("not collectible2 " + cardId);
            }
        }
    }
}
