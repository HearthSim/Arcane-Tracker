package net.mbonnin.arcanetracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */
public class Card implements Comparable<String> {
    public static final String classNameList[] = {"Warrior", "Shaman", "Rogue", "Paladin", "Hunter", "Druid", "Warlock", "Mage", "Priest", "Neutral"};

    public static final String RARITY_LEGENDARY="LEGENDARY";

    public static final String TYPE_HERO="HERO";
    public static final String TYPE_UNKNOWN="TYPE_UNKNOWN";
    public static final String TYPE_SPELL = "SPELL";
    public static final String TYPE_MINION = "MINION";
    public static final String TYPE_WEAPON = "WEAPON";
    public static final String TYPE_HERO_POWER = "HERO_POWER";
    public static final String TYPE_ENCHANTMENT = "ENCHANTMENT";

    public static final String RACE_MECHANICAL = "MECHANICAL";
    public static final String RACE_MURLOC = "MURLOC";
    public static final String RACE_DEMON = "DEMON";
    public static final String RACE_BEAST = "BEAST";
    public static final String RACE_TOTEM = "TOTEM";
    public static final String RACE_PIRATE = "PIRATE";
    public static final String RACE_DRAGON = "DRAGON";

    public static final int CLASS_INDEX_WARRIOR = 0;
    public static final int CLASS_INDEX_SHAMAN = 1;
    public static final int CLASS_INDEX_ROGUE = 2;
    public static final int CLASS_INDEX_PALADIN = 3;
    public static final int CLASS_INDEX_HUNTER = 4;
    public static final int CLASS_INDEX_DRUID = 5;
    public static final int CLASS_INDEX_WARLOCK = 6;
    public static final int CLASS_INDEX_MAGE = 7;
    public static final int CLASS_INDEX_PRIEST = 8;
    public static final int CLASS_INDEX_NEUTRAL = 9;

    public static final String CLASS_WARRIOR = "WARRIOR";
    public static final String CLASS_SHAMAN = "SHAMAN";
    public static final String CLASS_ROGUE = "ROGUE";
    public static final String CLASS_PALADIN = "PALADIN";
    public static final String CLASS_HUNTER = "HUNTER";
    public static final String CLASS_DRUID = "DRUID";
    public static final String CLASS_WARLOCK = "WARLOCK";
    public static final String CLASS_MAGE = "MAGE";
    public static final String CLASS_PRIEST = "PRIEST";
    public static final String CLASS_NEUTRAL = "NEUTRAL";

    public static final String ID_COINe = "GAME_005e";
    public static final String ID_COIN = "GAME_005";
    public static final String ID_GANG_UP = "BRM_007";
    public static final String ID_BENEATH_THE_GROUNDS = "AT_035";
    public static final String ID_AMBUSHTOKEN = "AT_035t";
    public static final String ID_IRON_JUGGERNAUT = "GVG_056";
    public static final String ID_BURROWING_MINE_TOKEN = "GVG_056t";
    public static final String ID_RECYCLE = "GVG_031";
    public static final String MANIC_SOULCASTER = "CFM_660";
    public static final String FORGOTTEN_TORCH = "LOE_002";
    public static final String ROARING_TORCH = "LOE_002t";
    public static final String CURSE_OF_RAFAAM = "LOE_007";
    public static final String CURSED = "LOE_007t";
    public static final String ANCIENT_SHADE = "LOE_110";
    public static final String ANCIENT_CURSE = "LOE_110t";
    public static final String EXCAVATED_EVIL = "LOE_111";
    public static final String ELISE = "LOE_079";
    public static final String MAP_TO_THE_GOLDEN_MONKEY = "LOE_019t";
    public static final String GOLDEN_MONKEY = "LOE_019t2";
    public static final String DOOMCALLER = "OG_255";
    public static final String CTHUN = "OG_280";
    public static final String JADE_IDOL = "CFM_602";
    public static final String WHITE_EYES = "CFM_324";
    public static final String STORM_GUARDIAN = "CFM_324t";

    public String name;
    public String playerClass;
    public String id;
    public String rarity;
    public String type;
    public String text;
    public String race;
    public String set;
    public String multiClassGroup;
    public int dbfId;

    public Integer cost;
    public Integer attack;
    public Integer health;
    public Integer durability;
    public Boolean collectible;

    @Override
    public int compareTo(String o) {
        return id.compareTo(o);
    }

    public static Card unknown() {
        Card card = new Card();
        card.name = "?";
        card.playerClass = "?";
        card.cost = -1;
        card.id = "?";
        card.rarity = "?";
        card.type = TYPE_UNKNOWN;
        card.text = "?";
        card.race = "?";
        card.collectible = false;
        return card;
    }

    @Override
    public String toString() {
        return name + "(" + id + ")";
    }

    public static int heroIdToClassIndex(String heroId) {

        Pattern pattern = Pattern.compile("hero_([0-9]*)[a-zA-Z]*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(heroId);
        if (matcher.matches()) {
            try {
                int i = Integer.parseInt(matcher.group(1));
                i--;

                if (i >= 0 && i < 9) {
                    return i;
                }

            } catch (Exception e) {
                Timber.e("wrong heroId" + heroId);
            }
        }

        return -1;
    }

    public static String classIndexToPlayerClass(int classIndex) {
        switch (classIndex) {
            case CLASS_INDEX_WARRIOR:
                return "WARRIOR";
            case CLASS_INDEX_ROGUE:
                return "ROGUE";
            case CLASS_INDEX_SHAMAN:
                return "SHAMAN";
            case CLASS_INDEX_PALADIN:
                return "PALADIN";
            case CLASS_INDEX_HUNTER:
                return "HUNTER";
            case CLASS_INDEX_DRUID:
                return "DRUID";
            case CLASS_INDEX_WARLOCK:
                return "WARLOCK";
            case CLASS_INDEX_MAGE:
                return "MAGE";
            case CLASS_INDEX_PRIEST:
                return "PRIEST";
            default:
                return "NEUTRAL";
        }
    }

    public static int niceNameToClassIndexNC(String niceName) {
        niceName = niceName.toLowerCase();
        for (int i = 0; i < classNameList.length; i++) {
            if (niceName.equals(classNameList[i].toLowerCase())) {
                return i;
            }
        }

        return CLASS_INDEX_NEUTRAL;
    }

    public static String classIndexToNiceName(int classIndex) {
        if (classIndex >= 0 && classIndex < classNameList.length) {
            return classNameList[classIndex];
        }

        return "?";
    }
    public static String classIndexToHeroId(int classIndex) {
        return String.format("hero_%02d", classIndex + 1);
    }

    public static int playerClassToClassIndex(String playerClass) {
        for (int i = 0; i < CLASS_INDEX_NEUTRAL; i++) {
            if (classIndexToPlayerClass(i).equals(playerClass)){
                return i;
            }
        }

        return -1;
    }

    public static boolean isCollectible(String cardId) {
        Card card = CardDb.getCard(cardId);
        if (card.collectible == null || !card.collectible) {
            return false;
        }

        if (Card.ID_COINe.equals(cardId) || Card.ID_COIN.equals(cardId)) {
            return false;
        }
        return true;
    }
}
