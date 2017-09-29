package net.mbonnin.arcanetracker;


import net.mbonnin.hsmodel.Card;
import net.mbonnin.hsmodel.CardJson;
import net.mbonnin.hsmodel.playerclass.PlayerClassKt;
import net.mbonnin.hsmodel.type.TypeKt;

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
        card.type = Card.UNKNOWN_TYPE;
        card.text = "?";
        card.race = "?";
        card.collectible = false;
        return card;
    }

    public static Card secret(String playerClass) {
        Card card = unknown();
        card.type = TypeKt.SPELL;
        card.text = Utils.getString(R.string.secretText);
        switch (playerClass) {
            case PlayerClassKt.PALADIN:
                card.id = "secret_p";
                card.cost = 1;
                card.playerClass = PlayerClassKt.PALADIN;
                break;
            case PlayerClassKt.HUNTER:
                card.id = "secret_h";
                card.cost = 2;
                card.playerClass = PlayerClassKt.HUNTER;
                break;
            case PlayerClassKt.MAGE:
                card.id = "secret_m";
                card.cost = 3;
                card.playerClass = PlayerClassKt.MAGE;
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
