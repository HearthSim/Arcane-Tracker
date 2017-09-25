package net.mbonnin.arcanetracker;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.mbonnin.arcanetracker.hsmodel.Card;
import net.mbonnin.arcanetracker.hsmodel.CardJson;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class GenerateCardIds {
    @Test
    public void generate() throws Exception {
        List<Card> list = CardJson.get().allCards();

        TreeMap<String, ArrayList<String>> map = new TreeMap<>();
        for (Card card: list) {
            try {
                String cardName = card.name
                        .toUpperCase()
                        .replaceAll(" ", "_")
                        .replaceAll("[^A-Z_]", "");
                ArrayList<String> allIds = map.get(cardName);
                if (allIds == null) {
                    allIds = new ArrayList<>();
                    map.put(cardName, allIds);
                }
                allIds.add(card.id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        SortedSet<String> keys = new TreeSet<>(map.keySet());
        for (String key : keys) {
            ArrayList<String> allXids = map.get(key);
            Collections.sort(allXids);

            int i = 0;
            for (String xid: allXids) {
                String name = key;
                if (i > 0) {
                    name = name + i;
                }
                names.add(name);
                ids.add(xid);
                i++;
            }
        }


        StringBuilder sb = new StringBuilder();
        sb.append("package net.mbonnin.arcanetracker.cardids;\n");
        sb.append("public final class CardIs {\n");
        for (int i = 0; i < names.size(); i++) {
            sb.append(String.format(Locale.ENGLISH, "public static final String %s = \"%s\";\n", names.get(i), ids.get(i)));
        }
        sb.append("}\n");
        System.out.print(sb.toString());
    }

}
