package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.hsmodel.Card;
import net.mbonnin.arcanetracker.hsmodel.CardJson;

import java.util.Collections;

/**
 * Created by martin on 9/25/17.
 */

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
        int classIndex = Card.niceNameToClassIndexNC(clazz);
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
        if (CardJson.allCards() == null) {
            return null;
        }

        for (Card card: CardJson.allCards()) {
            if (card.dbfId == dbfId) {
                return card;
            }
        }

        return null;
    }

    public static Card getCard(String key) {
        if (CardJson.allCards() == null) {
            /*
             * can happen  the very first launch
             * or maybe even later in some cases, the calling code does not check for null so we need to be robust to that
             */
            return UNKNOWN;
        }
        int index = Collections.binarySearch(CardJson.allCards(), key);
        if (index < 0) {
            return UNKNOWN;
        } else {
            return CardJson.allCards().get(index);
        }
    }
}
