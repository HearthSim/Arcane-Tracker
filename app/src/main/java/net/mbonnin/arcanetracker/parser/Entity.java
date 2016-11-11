package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;

import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/8/16.
 */

public class Entity {
    public static final String KEY_ZONE = "ZONE";
    public static final String KEY_CONTROLLER = "CONTROLLER";
    public static final String KEY_CARDTYPE = "CARDTYPE";
    public static final String KEY_FIRST_PLAYER = "FIRST_PLAYER";
    public static final String KEY_PLAYSTATE = "PLAYSTATE";

    public static final String PLAYSTATE_WON = "WON";

    public static final String ZONE_DECK = "DECK";
    public static final String ZONE_HAND = "HAND";

    public static final String CARDTYPE_HERO = "HERO";
    public static final String CARDTYPE_HERO_POWER = "HERO_POWER";

    public static final String ENTITY_ID_GAME = "1";

    public String EntityID;
    public String CardID; // might be null if the entity is not revealed yet
    public String PlayerID; // only valid for player entities

    public HashMap<String, String> tags = new HashMap();

    /**
     * extra information added by GameLogic
     */
    public HashMap<String, String> extra = new HashMap();
    public Card card;

    @Override
    public String toString() {
        return "CardEntity [id=" + EntityID + "][CardID=" + CardID + "]";
    }

    public void dump() {
        for (String key:tags.keySet()) {
            Timber.v("   " + key + "=" + tags.get(key));
        }
    }

}
