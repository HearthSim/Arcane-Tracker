package net.mbonnin.arcanetracker.parser;

import android.text.TextUtils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class PowerParser {
    private final Listener mListener;
    private Block mCurrentBlock;
    private int lastSpaces;
    private LinkedList<Node> mNodeStack = new LinkedList<Node>();

    public interface Listener {
        void onNewGame(Game game);

        void enEndGame(Game game);
    }

    private Game mCurrentGame;

    private final Pattern LINE = Pattern.compile("D ([^ ]*) ([^ ]*) - (.*)");

    private final Pattern BLOCK_START = Pattern.compile("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*)");
    private final Pattern BLOCK_END = Pattern.compile("BLOCK_END");

    private final Pattern GameEntityPattern = Pattern.compile("GameEntity EntityID=(.*)");
    private final Pattern PlayerEntityPattern = Pattern.compile("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)");

    private final Pattern FULL_ENTITY = Pattern.compile("FULL_ENTITY - Updating (.*) CardID=(.*)");
    private final Pattern SHOW_ENTITY = Pattern.compile("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)");
    private final Pattern HIDE_ENTITY = Pattern.compile("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG_CHANGE = Pattern.compile("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG = Pattern.compile("tag=(.*) value=(.*)");

    public PowerParser(String file, Listener listener) {
        mListener = listener;

        boolean readPreviousData = false;
        readPreviousData = true;
        new LogReader(file, line -> parsePowerLine(line), readPreviousData);

        mCurrentBlock = new Block();
    }

    private void parsePowerLine(String line) {
        Matcher m;

        m = LINE.matcher(line);
        if (!m.matches()) {
            Timber.e("invalid line: " + line);
            return;
        }

        String tag = m.group(2);
        if (tag.equals("PowerTaskList.DebugPrintPower()")) {
            parsePowerTaskList(m.group(3));
        }
    }

    static class Block {
        String blockType;
        String entity;
        String effectCardId;
        String effectIndex;
        String target;
    }

    static class Node {
        String line;
        ArrayList<Node> children = new ArrayList<>();
    }

    void parsePowerTaskList(String line) {
        Timber.v(line);

        int spaces = 0;
        while (spaces < line.length() && line.charAt(spaces) == ' ') {
            spaces++;
        }

        if (spaces == line.length()) {
            Timber.e("empty line: " + line);
            return;
        }

        line = line.substring(spaces);

        Matcher m;

        if (spaces <= 0) {
            output(mNodeStack.getFirst());
            mNodeStack.clear();
        }

        if (spaces == 0) {
            if ((m = BLOCK_START.matcher(line)).matches()) {
                mCurrentBlock.blockType = m.group(1);
                mCurrentBlock.entity = m.group(2);
                mCurrentBlock.effectCardId = m.group(3);
                mCurrentBlock.effectIndex = m.group(4);
                mCurrentBlock.target = m.group(5);
            } else if ((m = BLOCK_END.matcher(line)).matches()) {
                mCurrentBlock.blockType = null;
            } else {
                Timber.e("unknown block: " + line);
            }
        } else if (spaces >= 4) {
            Node parent;

            if (mNodeStack.isEmpty()) {
                parent = null;
            } else {
                parent = mNodeStack.peekLast();
            }

            Node node = new Node();
            node.line = line;

            int diff = (spaces - lastSpaces) / 4;
            if (diff > 0) {
                if (diff != 1) {
                    Timber.e("bad nesting: " + line);
                    return;
                }
                if (parent != null) {
                    parent.children.add(node);
                }
                mNodeStack.addLast(node);
            } else {
                while (diff <= 0) {
                    diff++;
                    if (!mNodeStack.isEmpty()) {
                        parent = mNodeStack.removeLast();
                    } else {
                        Timber.e("bad nesting: " + line);
                        return;
                    }
                }
                parent.children.add(node);
                mNodeStack.addLast(node);
            }
        }
        lastSpaces = spaces;
    }


    HashMap<String, String> getTags(Node node) {
        HashMap<String, String> map = new HashMap<>();

        for (Node child: node.children) {
            Matcher m = TAG.matcher(child.line);
            if (m.matches()) {
                String key = m.group(1);
                if (key != null) {
                    map.put(key, m.group(2));
                }
            }
        }

        return map;
    }

    /**
     * break down the entities to their respective players
     */
    private void updateState() {
        HashMap<String, Entity> entityMap = mCurrentGame.getEntities();

        mCurrentGame.getPlayer("1").reset();
        mCurrentGame.getPlayer("2").reset();

        for (String key: entityMap.keySet()) {
            Entity entity = entityMap.get(key);

            if (!(entity instanceof CardEntity)) {
                continue;
            }
            CardEntity cardEntity = (CardEntity)entity;

            String playerId = entity.tags.get("CONTROLLER");
            String CARDTYPE = entity.tags.get("CARDTYPE");

            if (TextUtils.isEmpty(playerId)) {
                Timber.e("no player for:");
                entity.dump();
                continue;
            }

            Player player = mCurrentGame.getPlayer(playerId);

            if ("HERO".equals(CARDTYPE)) {
                player.hero = cardEntity;
            } else if ("HERO_POWER".equals(CARDTYPE)) {
                player.heroPower = cardEntity;
            } else {
                String zone = entity.tags.get("ZONE");
                if (TextUtils.isEmpty(zone)) {
                    Timber.v("unknown zone for:");
                    entity.dump();
                    continue;
                }

                player.cards.add(cardEntity);
            }
        }

        for (Player player: mCurrentGame.getPlayerList()) {
            for (Player.Listener listener: player.listeners) {
                listener.onPlayerStateChanged();
            }
        }
    }

    private String lookupEntityId(String name) {
        if (TextUtils.isEmpty(name)) {
            return lookupUnknown();
        } else if (name.length() >= 2 && name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']') {
            HashMap<String, String> params = decodeParams(name.substring(1, name.length() - 1));
            String id = params.get("id");
            if (TextUtils.isEmpty(id)) {
                return lookupUnknown();
            } else {
                return id;
            }
        } else if ("GameEntity".equals(name)) {
            return "1";
        } else {
            // this must be a battleTag
            Entity entity = mCurrentGame.getEntity(name);
            if (entity == null) {
                Timber.w("Adding battleTag " + name);
                mCurrentGame.battleTags.add(name);

                entity = new Entity();
                entity.EntityID = name;
                mCurrentGame.setEntity(entity);
            }
            return name;
        }
    }

    private String lookupUnknown() {
        Entity unknownEntity = mCurrentGame.getEntity("unknown");
        if (unknownEntity == null) {
            unknownEntity = new Entity();
            unknownEntity.EntityID = "unknown";
            mCurrentGame.setEntity(unknownEntity);
        }
        return "unknown";
    }

    private void detectPlayers() {
        updateState();

        int knownCardsInHand = 0;
        int totalCardsInHand = 0;
        Player player1 = mCurrentGame.getPlayer("1");

        for (CardEntity entity: player1.cards) {
            if (!"HAND".equals(entity.tags.get("ZONE"))) {
                continue;
            }
            if (!TextUtils.isEmpty(entity.CardID)) {
                knownCardsInHand++;
            }
            totalCardsInHand++;
        }

        player1.isOpponent = knownCardsInHand < 3;
        player1.hasCoin = totalCardsInHand > 4;

        Player player2 = mCurrentGame.getPlayer("1");
        player2.isOpponent = !player1.isOpponent;
        player2.hasCoin = !player1.hasCoin;

        /**
         * now try to match a battle tag with a player
         */
        for (String battleTag: mCurrentGame.battleTags) {
            Entity entity = mCurrentGame.getEntity(battleTag);
            String first = entity.tags.get("FIRST_PLAYER");
            Player player;

            if ("1".equals(first)) {
                player = player1.hasCoin ? player2: player1;
            } else {
                player = player1.hasCoin ? player1: player2;
            }

            player.entity.tags.putAll(entity.tags);
            player.battleTag = battleTag;

            /**
             * make the battleTag point to the same entity..
             */
            Timber.w(battleTag + " now points to entity " + entity.EntityID);
            mCurrentGame.setEntity(battleTag, entity);
        }

        mCurrentGame.player = player1.isOpponent ? player2:player1;
        mCurrentGame.opponent = player1.isOpponent ? player1:player2;
    }
    private void output(Node node) {
        String line = node.line;

        Matcher m;

        if (line.startsWith("CREATE_GAME")) {
            if (mCurrentGame != null) {
                Timber.w("CREATE_GAME during an existing one, resuming");
            } else {
                mCurrentGame = new Game();
                for (Node child:node.children) {
                    if ((m = GameEntityPattern.matcher(child.line)).matches()) {
                        GameEntity entity = new GameEntity();
                        entity.EntityID = m.group(1);
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.entity = entity;
                        mCurrentGame.setEntity(entity);
                    } else if ((m = PlayerEntityPattern.matcher(child.line)).matches()) {
                        String PlayerID = m.group(2);
                        PlayerEntity entity = new PlayerEntity();
                        entity.EntityID = m.group(1);
                        entity.PlayerID = PlayerID;
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.setEntity(entity);

                        mCurrentGame.getPlayer(PlayerID).entity = entity;
                    }
                }
            }
        }

        if (mCurrentGame == null) {
            return;
        }

        if ((m = TAG_CHANGE.matcher(line)).matches()) {
            String entityId = lookupEntityId(m.group(1));
            String key = m.group(2);
            String value = m.group(3);

            if (key != null && !"".equals(key)) {
                mCurrentGame.getEntity(entityId).tags.put(key, value);

                if (entityId.equals("1") && "STEP".equals(key)) {
                    if ("BEGIN_MULLIGAN".equals(value)) {
                        detectPlayers();
                        mListener.onNewGame(mCurrentGame);
                    } else if ("FINAL_GAMEOVER".equals(value)) {
                        mListener.enEndGame(mCurrentGame);
                    }
                }
            }
        } else if ((m = FULL_ENTITY.matcher(line)).matches()) {
            CardEntity entity = new CardEntity();
            entity.EntityID = m.group(1);
            entity.CardID = m.group(2);
            entity.tags.putAll(getTags(node));

            mCurrentGame.setEntity(entity);
        } else if ((m = SHOW_ENTITY.matcher(line)).matches()) {
            String entityId = lookupEntityId(m.group(1));
            Entity entity = mCurrentGame.getEntity(entityId);
            if (entity instanceof CardEntity) {
                CardEntity cardEntity = (CardEntity)entity;
                String CardID = m.group(2);
                if (!TextUtils.isEmpty(cardEntity.CardID) && !cardEntity.CardID.equals(CardID)) {
                    Timber.e("entity " + entityId + " changed cardId " + cardEntity.CardID + " -> " + CardID);
                }
                cardEntity.CardID = CardID;
                entity.tags.putAll(getTags(node));
            } else {
                Timber.e("not a CardEntity ?");
                entity.dump();
            }
            updateState();
        } else if ((m = HIDE_ENTITY.matcher(line)).matches()) {
            String entityId = lookupEntityId(m.group(1));
            Entity entity = mCurrentGame.getEntity(entityId);

            String key = m.group(2);
            if (!TextUtils.isEmpty(key)) {
                entity.tags.put(key, m.group(3));
            }
            updateState();
        }
    }

    private static HashMap<String, String> allParams(String line) {
        HashMap<String, String> map = new HashMap<>();

        String result[] = splitLine(line);
        for (int i = 1; i <= 2; i++) {
            if (result[i] != null) {
                HashMap<String, String> map2 = decodeParams(result[i]);
                for (String key : map2.keySet()) {
                    if (map2.get(key) != null && !map2.get(key).equals("")) {
                        map.put(key, map2.get(key));
                    }
                }
            }
        }

        return map;
    }

    private static HashMap<String, String> decodeParams(String params) {
        int end = params.length();
        HashMap<String, String> map = new HashMap<>();

        while (true) {
            int start = end - 1;

            String value;
            while (start >= 0 && params.charAt(start) != '=') {
                start--;
            }
            if (start < 0) {
                return map;
            }
            value = params.substring(start + 1, end);
            end = start;
            if (end < 0) {
                return map;
            }
            start = end - 1;
            while (start >= 0 && params.charAt(start) != ' ') {
                start--;
            }
            String key;
            if (start == 0) {
                key = params.substring(start, end);
            } else {
                key = params.substring(start + 1, end);
            }
            map.put(key.trim(), value);
            if (start == 0) {
                break;
            } else {
                end = start;
            }
        }

        return map;
    }

    private static String[] splitLine(String line) {
        int i = 0;
        String result[] = new String[3];
        while (i < line.length() && line.charAt(i) != '[') {
            i++;
        }
        result[0] = line.substring(0, i);
        if (i == line.length()) {
            return result;
        }

        i++;
        int end = i;
        while (end < line.length() && line.charAt(end) != ']') {
            end++;
        }
        result[1] = line.substring(i, end);
        if (end >= line.length() - 1) {
            return result;
        }
        result[2] = line.substring(end + 1, line.length());

        return result;
    }

    private static HashMap<String, String> arrayParams(String line) {
        String split[] = splitLine(line);
        if (split[1] != null) {
            return decodeParams(split[1]);
        }

        return new HashMap<>();
    }

}
