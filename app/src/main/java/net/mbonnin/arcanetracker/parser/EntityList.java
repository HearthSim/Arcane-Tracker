package net.mbonnin.arcanetracker.parser;

import android.text.TextUtils;

import com.android.internal.util.Predicate;

import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class EntityList extends ArrayList<Entity> {
    public static final Predicate<Entity> HAS_CARD_ID = entity -> !TextUtils.isEmpty(entity.CardID);
    public static final Predicate<Entity> IS_IN_DECK = entity -> Entity.ZONE_DECK.equals(entity.tags.get(Entity.KEY_ZONE));
    public static final Predicate<Entity> IS_OUTSIDE_DECK = entity -> !IS_IN_DECK.apply(entity);
    public static final Predicate<Entity> IS_FROM_ORIGINAL_DECK = entity -> Entity.TRUE.equals(entity.extra.get(Entity.EXTRA_KEY_ORIGINAL_DECK));
    public static final Predicate<Entity> IS_NOT_FROM_ORIGINAL_DECK = entity -> !IS_FROM_ORIGINAL_DECK.apply(entity);

    public EntityList filter(Predicate<Entity> predicate) {
        EntityList list = new EntityList();
        for (Entity entity : this) {
            if (predicate.apply(entity)) {
                list.add(entity);
            }
        }
        return list;
    }

    public HashMap<String, Integer> toCardMap() {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (Entity entity : filter(HAS_CARD_ID)) {
            Utils.cardMapAdd(map, entity.CardID, 1);
        }
        return map;
    }
}
