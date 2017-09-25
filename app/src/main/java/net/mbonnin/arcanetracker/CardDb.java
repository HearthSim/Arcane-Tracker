package net.mbonnin.arcanetracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.mbonnin.arcanetracker.hsmodel.Card;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class CardDb {
    private static ArrayList<Card> sCardList;

    private static String getAssetName(String lang) {
        return "cards_" + lang + ".json";
    }

    public static Card getCard(int dbfId) {
        if (sCardList == null) {
            return null;
        }

        for (Card card: sCardList) {
            if (card.dbfId == dbfId) {
                return card;
            }
        }

        return null;
    }

    public static Card getCard(String key) {
        if (sCardList == null) {
            /*
             * can happen  the very first launch
             * or maybe even later in some cases, the calling code does not check for null so we need to be robust to that
             */
            return CardUtil.UNKNOWN;
        }
        int index = Collections.binarySearch(sCardList, key);
        if (index < 0) {
            return CardUtil.UNKNOWN;
        } else {
            return sCardList.get(index);
        }
    }

    public static ArrayList<Card> getCards() {
        if (sCardList == null) {
            return new ArrayList<>();
        }
        return sCardList;
    }

    public static void init(ArrayList<Card> list) {
        if (list == null) {
            list = new ArrayList<>();
        }

        Collections.sort(list, (a, b) -> a.id.compareTo(b.id));

        sCardList = list;
    }

    public static void init() {
        String jsonName = Language.getCurrentLanguage().jsonName;

        String cards = getStoredJson(jsonName);
        ArrayList<Card> list = new Gson().fromJson(cards, new TypeToken<ArrayList<Card>>() {}.getType());

        /*
         * these are 3 fake cards needed for CardRender
         */
        list.add(CardUtil.secret("PALADIN"));
        list.add(CardUtil.secret("HUNTER"));
        list.add(CardUtil.secret("MAGE"));

        init(list);
    }

    private static String getStoredJson(String lang) {
        InputStream inputStream;
        try {
            inputStream = ArcaneTrackerApplication.getContext().getAssets().open(getAssetName(lang));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try {
            return Utils.inputStreamToString(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
