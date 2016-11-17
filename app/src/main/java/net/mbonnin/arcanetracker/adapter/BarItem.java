package net.mbonnin.arcanetracker.adapter;

import net.mbonnin.arcanetracker.Card;

import java.util.Comparator;

/**
 * Created by martin on 11/8/16.
 */
public class BarItem {
    static public Comparator<BarItem> COMPARATOR = (a, b) -> {
        int acost = a.card.cost == null ? 0: a.card.cost;
        int bcost = b.card.cost == null ? 0: b.card.cost;

        int ret = acost - bcost;
        if (ret == 0) {
            ret = a.card.name.compareTo(b.card.name);
        }
        return ret;
    };
    public Card card;
    public int count;
    public boolean gift;
}
