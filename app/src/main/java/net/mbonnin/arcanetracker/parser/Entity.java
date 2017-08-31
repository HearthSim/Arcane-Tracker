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
    public static final String KEY_DEFENDING = "DEFENDING";
    public static final String KEY_CLASS = "CLASS";
    public static final String KEY_RARITY = "RARITY";
    public static final String KEY_CURRENT_PLAYER = "CURRENT_PLAYER";

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

    public static final String STEP_FINAL_GAMEOVER = "FINAL_GAMEOVER";
    public static final String STEP_BEGIN_MULLIGAN = "BEGIN_MULLIGAN";
    public static final String RARITY_EPIC = "EPIC";
    public static final String RARITY_LEGENDARY = "LEGENDARY";

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
        return String.format(Locale.ENGLISH, "CardEntity [id=%s][CardID=%s]%s", EntityID, CardID, card != null ? "(" + card.name + ")" : "");
    }

    public static class Extra {
        /**
         * used from Controller.java to affect a temporary id to cards we don't know yet
         */
        public Card tmpCard;
        public boolean tmpIsGift;

        public String originalController;
        public int drawTurn = -1;
        public int playTurn = -1;
        public int diedTurn = -1;
        public boolean mulliganed;
        public String createdBy;

        /*
         * secret detector
         */
        public boolean otherPlayerPlayedMinion;
        public boolean otherPlayerCastSpell;
        public boolean otherPlayerHeroPowered;
        public boolean selfHeroAttacked;
        public boolean selfMinionWasAttacked;
        public boolean selfHeroDamaged;
        public boolean selfPlayerMinionDied;
        public boolean selfMinionTargetedBySpell;
        public boolean competitiveSpiritTriggerConditionHappened;
        public boolean otherPlayerPlayedMinionWithThreeOnBoardAlready;
    }

    public void dump() {
        for (String key : tags.keySet()) {
            Timber.v("   " + key + "=" + tags.get(key));
        }
    }

    public Entity clone() {
        Entity clone = new Entity();
        clone.EntityID = EntityID;
        clone.PlayerID = PlayerID;
        clone.tags.putAll(tags);
        clone.card = card;
        clone.CardID = CardID;
        clone.extra.drawTurn = extra.drawTurn;
        clone.extra.playTurn = extra.playTurn;
        clone.extra.createdBy = extra.createdBy;
        clone.extra.mulliganed = extra.mulliganed;

        clone.extra.otherPlayerPlayedMinion = extra.otherPlayerPlayedMinion;
        clone.extra.otherPlayerCastSpell = extra.otherPlayerCastSpell;
        clone.extra.otherPlayerHeroPowered = extra.otherPlayerHeroPowered;
        clone.extra.selfHeroAttacked = extra.selfHeroAttacked;
        clone.extra.selfMinionWasAttacked = extra.selfMinionWasAttacked;
        clone.extra.selfHeroDamaged = extra.selfHeroDamaged;
        clone.extra.selfPlayerMinionDied = extra.selfPlayerMinionDied;
        clone.extra.selfMinionTargetedBySpell = extra.selfMinionTargetedBySpell;
        clone.extra.competitiveSpiritTriggerConditionHappened = extra.competitiveSpiritTriggerConditionHappened;
        clone.extra.otherPlayerPlayedMinionWithThreeOnBoardAlready = extra.otherPlayerPlayedMinionWithThreeOnBoardAlready;
        return clone;
    }

}
