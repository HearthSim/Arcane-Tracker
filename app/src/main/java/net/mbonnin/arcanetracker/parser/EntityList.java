package net.mbonnin.arcanetracker.parser;

import android.text.TextUtils;

import com.android.internal.util.Predicate;

import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class EntityList extends ArrayList<Entity> {
    public static final Predicate<Entity> IS_IN_DECK = new ZonePredicate(Entity.ZONE_DECK);
    public static final Predicate<Entity> IS_NOT_IN_DECK = new NegatePredicate(IS_IN_DECK);
    public static final Predicate<Entity> IS_IN_HAND = new ZonePredicate(Entity.ZONE_HAND);

    public static final Predicate<Entity> HAS_CARD_ID = entity -> !TextUtils.isEmpty(entity.CardID);
    private static final Predicate<Entity> IS_ENCHANTMENT = new CardTypePredicate(Entity.CARDTYPE_ENCHANTMENT);
    public static final Predicate<Entity> IS_NOT_ENCHANTMENT = new NegatePredicate(IS_ENCHANTMENT);

    public static class CardTypePredicate implements Predicate<Entity> {
        private final String mCardType;

        CardTypePredicate(String cardType) {
            mCardType = cardType;
        }
        @Override
        public boolean apply(Entity entity) {
            return mCardType.equals(entity.tags.get(Entity.KEY_CARDTYPE));
        }
    }

    public static class ZonePredicate implements Predicate<Entity> {
        private String mZone;

        public ZonePredicate(String z) {
            mZone = z;
        }
        @Override
        public boolean apply(Entity entity) {
            return mZone.equals(entity.tags.get(Entity.KEY_ZONE));
        }
    }

    public static class NegatePredicate implements Predicate<Entity> {
        private Predicate<Entity> mPredicate;

        public NegatePredicate(Predicate<Entity> p) {
            mPredicate = p;
        }
        @Override
        public boolean apply(Entity entity) {
            return !mPredicate.apply(entity);
        }
    }


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
