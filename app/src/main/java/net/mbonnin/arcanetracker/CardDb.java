package net.mbonnin.arcanetracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.mbonnin.arcanetracker.hsmodel.Card;
import net.mbonnin.arcanetracker.hsmodel.CardJson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class CardDb {

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
            return CardUtil.UNKNOWN;
        }
        int index = Collections.binarySearch(CardJson.allCards(), key);
        if (index < 0) {
            return CardUtil.UNKNOWN;
        } else {
            return CardJson.allCards().get(index);
        }
    }


    public static void init() {
        String jsonName = Language.getCurrentLanguage().jsonName;

        ArrayList<Card> injectedCards = new ArrayList<>();

        /*
         * these are 3 fake cards needed for CardRender
         */
        injectedCards.add(CardUtil.secret("PALADIN"));
        injectedCards.add(CardUtil.secret("HUNTER"));
        injectedCards.add(CardUtil.secret("MAGE"));

        CardJson.init(jsonName, injectedCards);
    }
}
