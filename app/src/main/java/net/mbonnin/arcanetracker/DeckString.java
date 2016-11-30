package net.mbonnin.arcanetracker;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import timber.log.Timber;

public class DeckString {
    public static Deck parse(String pasteData) {
        String lines[] = pasteData.split("\n");
        Deck deck = null;

        String name = "imported deck";
        for (String line: lines) {
            if (line.startsWith("### ")) {
                name = line.substring(4);
            } else if (!line.startsWith("#")) {
                try {
                    deck = decodeCards(line);
                } catch (Exception e) {
                    Timber.e(e);
                }
            }
        }

        if (deck == null) {
            return null;
        }
        deck.name = name;

        if (deck.classIndex < 0) {
            return null;
        }

        if (deck.cards == null) {
            return null;
        }

        return deck;
    }

    private static Deck decodeCards(String deckstring) {
        byte[] data = Base64.decode(deckstring, Base64.DEFAULT);

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        Timber.d("deckstring: %s", deckstring);

        Deck deck = new Deck();
        deck.id = UUID.randomUUID().toString();
        deck.classIndex = -1;

        byteBuffer.get(); // reserverd
        byteBuffer.get(); // version;
        VarInt.getVarInt(byteBuffer); // wild/standard

        int heroCount = VarInt.getVarInt(byteBuffer);
        for (int i = 0; i < heroCount; i++) {
            Card card = CardDb.getCard(VarInt.getVarInt(byteBuffer));
            if (card != null) {
                deck.classIndex = Card.playerClassToClassIndex(card.playerClass);
            }
        }

        deck.cards = new HashMap<>();
        int cardCount = VarInt.getVarInt(byteBuffer);
        for (int i = 0; i < cardCount; i++) {
            Card card = CardDb.getCard(VarInt.getVarInt(byteBuffer));
            if (card != null) {
                Timber.d("card1: %s", card.name);
                deck.cards.put(card.id, 1);
            }
        }

        cardCount = VarInt.getVarInt(byteBuffer);
        for (int i = 0; i < cardCount; i++) {
            Card card = CardDb.getCard(VarInt.getVarInt(byteBuffer));
            if (card != null) {
                Timber.d("card2: %s", card.name);
                deck.cards.put(card.id, 2);
            }
        }

        cardCount = VarInt.getVarInt(byteBuffer);
        for (int i = 0; i < cardCount; i++) {
            Card card = CardDb.getCard(VarInt.getVarInt(byteBuffer));
            int c = VarInt.getVarInt(byteBuffer);
            if (card != null) {
                deck.cards.put(card.id, c);
            }
        }

        return deck;
    }
}
