package net.mbonnin.arcanetracker;


import net.mbonnin.hsmodel.Card;
import net.mbonnin.hsmodel.CardJson;

import java.util.Collections;

public class CardUtil {
    public static final Card UNKNOWN = unknown();

    public static Card unknown() {
        Card card = new Card();
        card.name = "?";
        card.playerClass = "?";
        card.cost = Card.UNKNOWN_COST;
        card.id = "?";
        card.rarity = "?";
        card.type = Card.TYPE_UNKNOWN;
        card.text = "?";
        card.race = "?";
        card.collectible = false;
        return card;
    }

    public static Card secret(String clazz) {
        Card card = unknown();
        card.type = Card.TYPE_SPELL;
        card.text = Utils.getString(R.string.secretText);
        int classIndex = Card.Companion.niceNameToClassIndexNC(clazz);
        switch (classIndex) {
            case Card.CLASS_INDEX_HUNTER:
                card.id = "secret_h";
                card.cost = 2;
                card.playerClass = Card.CLASS_HUNTER;
                break;
            case Card.CLASS_INDEX_MAGE:
                card.id = "secret_m";
                card.cost = 3;
                card.playerClass = Card.CLASS_MAGE;
                break;
            case Card.CLASS_INDEX_PALADIN:
                card.id = "secret_p";
                card.cost = 1;
                card.playerClass = Card.CLASS_PALADIN;
                break;
        }
        card.name = Utils.getString(R.string.secret);
        return card;
    }

    public static Card getCard(int dbfId) {

        for (Card card: CardJson.INSTANCE.allCards()) {
            if (card.dbfId == dbfId) {
                return card;
            }
        }

        return null;
    }

    public static Card getCard(String key) {
        int index = Collections.binarySearch(CardJson.INSTANCE.allCards(), key);
        if (index < 0) {
            return UNKNOWN;
        } else {
            return CardJson.INSTANCE.allCards().get(index);
        }
    }
}
