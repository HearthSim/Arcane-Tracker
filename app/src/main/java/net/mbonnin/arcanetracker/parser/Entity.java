package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;

import java.util.HashMap;
import java.util.Locale;

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
    public static final String KEY_STEP = "STEP";
    public static final String KEY_TURN = "TURN";
    public static final String KEY_ZONE_POSITION = "ZONE_POSITION";

    public static final String PLAYSTATE_WON = "WON";

    public static final String ZONE_DECK = "DECK";
    public static final String ZONE_HAND = "HAND";
    public static final String ZONE_PLAY = "PLAY";
    public static final String ZONE_GRAVEYARD = "GRAVEYARD";
    public static final String ZONE_SECRET = "SECRET";

    public static final String CARDTYPE_HERO = Card.TYPE_HERO;
    public static final String CARDTYPE_HERO_POWER = Card.TYPE_HERO_POWER;
    public static final String CARDTYPE_ENCHANTMENT = Card.TYPE_ENCHANTMENT;
    public static final String CARDTYPE_WEAPON = Card.TYPE_WEAPON;
    public static final String CARDTYPE_MINION = Card.TYPE_MINION;

    public static final String ENTITY_ID_GAME = "1";

    public static final String STEP_FINAL_GAMEOVER= "FINAL_GAMEOVER";
    public static final String STEP_BEGIN_MULLIGAN = "BEGIN_MULLIGAN";

    public String EntityID;
    public String CardID; // might be null if the entity is not revealed yet
    public String PlayerID; // only valid for player entities

    public HashMap<String, String> tags = new HashMap();

    /**
     * extra information added by us
     */
    public Extra extra = new Extra();
    public Card card;

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "CardEntity [id=%s][CardID=%s]%s", EntityID, CardID, card != null ? "(" + card.name + ")" :"");
    }

    public static class Extra {
        /**
         * used from Controller.java to affect a temporary id to cards we don't know yet
         */
        public String tmpCardId;
        public String originalController;
        public int drawTurn = -1;
        public int playTurn = -1;
        public int diedTurn = -1;
        public boolean mulliganed;
    }

    public void dump() {
        for (String key:tags.keySet()) {
            Timber.v("   " + key + "=" + tags.get(key));
        }
    }

}
