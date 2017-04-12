package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.ArenaParser;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ParserListenerArena implements ArenaParser.Listener {

    private final MainViewCompanion mainViewCompanion;
    private final DeckListManager deckListManager;
    private final CardDb cardDb;

    public ParserListenerArena(MainViewCompanion mainViewCompanion, DeckListManager deckListManager, CardDb cardDb) {
        this.mainViewCompanion = mainViewCompanion;
        this.deckListManager = deckListManager;
        this.cardDb = cardDb;
    }

    @Override
    public void clear() {
        Deck deck = deckListManager.getArenaDeck();
        deck.clear();
        deckListManager.saveArena();
    }

    @Override
    public void arenaDraftStarted(int classIndex) {
        Deck deck = deckListManager.getArenaDeck();
        deck.classIndex = classIndex;
        mainViewCompanion.getPlayerCompanion().setDeck(deck, null);
    }

    @Override
    public void addCard(String cardId) {
        Deck deck = deckListManager.getArenaDeck();
        deck.addCard(cardId, 1);
        deckListManager.saveArena();
    }

    @Override
    public void addIfNotAlreadyThere(String cardId) {
        Deck deck = deckListManager.getArenaDeck();
        Card card = cardDb.getCard(cardId);
        if (!deck.cards.containsKey(cardId)) {
            if (card.collectible) {
                deck.addCard(cardId, 1);
                deckListManager.saveArena();
            } else {
                Timber.e("not collectible2 " + cardId);
            }
        }
    }
}
