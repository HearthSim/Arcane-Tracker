package net.mbonnin.arcanetracker.hsmodel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by martin on 9/25/17.
 */

public class CardJson {
    private static ArrayList<Card> list = null;

    public static void init(String lang, List<Card> injectedCards) {
        InputStream inputStream = CardJson.class.getResourceAsStream("/cards_" + lang + ".json");
        InputStreamReader reader = new InputStreamReader(inputStream);
        list = new Gson().fromJson(reader, new TypeToken<ArrayList<Card>>() {}.getType());
        list.addAll(injectedCards);

        Collections.sort(list, (a, b) -> a.id.compareTo(b.id));
    }

    public static List<Card> allCards() {
        return list;
    }
}
