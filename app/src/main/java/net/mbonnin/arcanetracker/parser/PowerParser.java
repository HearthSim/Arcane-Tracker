package net.mbonnin.arcanetracker.parser;

import android.text.TextUtils;


import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class PowerParser implements LogReader.LineConsumer {
    private final CardDb cardDb;
    private final Listener mListener;
    private LinkedList<Node> mNodeStack = new LinkedList<Node>();
    private Node mCurrentNode;

    private Game mCurrentGame;

    private final Pattern BLOCK_START = Pattern.compile("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*)");
    private final Pattern BLOCK_END = Pattern.compile("BLOCK_END");

    private final Pattern GameEntityPattern = Pattern.compile("GameEntity EntityID=(.*)");
    private final Pattern PlayerEntityPattern = Pattern.compile("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)");

    private final Pattern FULL_ENTITY = Pattern.compile("FULL_ENTITY - Updating (.*) CardID=(.*)");
    private final Pattern SHOW_ENTITY = Pattern.compile("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)");
    private final Pattern HIDE_ENTITY = Pattern.compile("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG_CHANGE = Pattern.compile("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG = Pattern.compile("tag=(.*) value=(.*)");

    static class Block {
        public static final String TYPE_PLAY="PLAY";
        public static final String TYPE_POWER="POWER";
        public static final String TYPE_TRIGGER="TRIGGER";

        String blockType;
        String entity;
        String effectCardId;
        String effectIndex;
        String target;
    }

    static class Node {
        String line;
        ArrayList<Node> children = new ArrayList<>();
        public int depth;
    }

    public interface Listener {
        void onGameStarted(Game game);
        void onGameEnded(Game game, boolean victory);
    }

    public PowerParser(CardDb cardDb, Listener listener) {
        this.cardDb = cardDb;
        this.mListener = listener;
    }

    public void onLine(String rawLine, int seconds, String line) {
        String s[] = Utils.extractMethod(line);

        if (s == null) {
            Timber.e("Cannot parse line: " + line);
            return;
        }

        if (!"PowerTaskList.DebugPrintPower()".equals(s[0])) {
            return;
        }

        line = s[1];

        Timber.v("PowerTaskList" + line);

        int spaces = 0;
        while (spaces < line.length() && line.charAt(spaces) == ' ') {
            spaces++;
        }

        if (spaces == line.length()) {
            Timber.e("empty line: " + line);
            return;
        } else if (spaces %4 != 0) {
            Timber.e("bad indentation: " + line);
            return;
        }

        line = line.substring(spaces);

        int depth = spaces/4;

        Node node = new Node();
        node.depth = depth;
        node.line = line;

        Node parent = null;
        while (!mNodeStack.isEmpty()) {
            Node node2 = mNodeStack.peekLast();
            if (depth == node2.depth + 1) {
                parent = node2;
                break;
            }
            mNodeStack.removeLast();
        }
        if (parent == null) {
            outputCurrentNode();
            mCurrentNode = node;
        } else if (BLOCK_END.matcher(parent.line).matches()) {
            /**
             * BLOCK_END is a special case :-/
             */
            outputCurrentNode();
            mCurrentNode = node;
        } else {
            parent.children.add(node);
        }

        mNodeStack.add(node);
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


    private void outputCurrentNode() {
        if (mCurrentNode == null) {
            return;
        }

        Node node = mCurrentNode;
        mCurrentNode = null;
        mNodeStack.clear();

        String line = node.line;

        Matcher m;

        if (node.depth == 0) {
            if ((m = BLOCK_START.matcher(line)).matches()) {
                Block block = new Block();
                block.blockType = m.group(1);
                block.entity = m.group(2);
                block.effectCardId = m.group(3);
                block.effectIndex = m.group(4);
                block.target = m.group(5);

                for (Node child: node.children) {
                    outputAction(block, child);
                }

                if (mCurrentGame != null && Block.TYPE_PLAY.equals(block.blockType)) {
                    Entity entity = findEntityByName(mCurrentGame, m.group(2));
                    GameLogic.entityPlayed(mCurrentGame, entity);
                }
            } else if ((m = BLOCK_END.matcher(line)).matches()) {
            } else {
                Timber.e("unknown block: " + line);
            }

            return;
        } else if (node.depth == 1) {
            outputAction(null, node);
        } else {
            Timber.e("ignore block" + line);
        }
    }

    private boolean isGameValid(Game game) {
        return mCurrentGame.player != null && mCurrentGame.opponent != null;
    }
    private void tagChange(Entity entity, String key, String newValue) {
        String oldValue = entity.tags.get(Entity.KEY_ZONE);
        entity.tags.put(key, newValue);

        GameLogic.tagChanged(mCurrentGame, entity, key, oldValue, newValue);

        if (!mCurrentGame.started && isGameValid(mCurrentGame)) {
            mListener.onGameStarted(mCurrentGame);
            mCurrentGame.started = true;
        }

        /**
         * Do not crash If we get a disconnect before the mulligan (might happen ?)
         */
        if (Entity.ENTITY_ID_GAME.equals(entity.EntityID) && mCurrentGame.started) {
            if (Entity.KEY_STEP.equals(key)) {
                if (Entity.STEP_FINAL_GAMEOVER.equals(newValue)) {
                    boolean victory = Entity.PLAYSTATE_WON.equals(mCurrentGame.player.entity.tags.get(Entity.KEY_PLAYSTATE));
                    mListener.onGameEnded(mCurrentGame, victory);

                    mCurrentGame = null;
                }
            }
        }
    }

    private void outputAction(Block block, Node node) {
        String line = node.line;
        Matcher m;

        if (line.startsWith("CREATE_GAME")) {
            if (mCurrentGame != null) {
                Timber.w("CREATE_GAME during an existing one, resuming");
            } else {
                mCurrentGame = new Game(cardDb);
                for (Node child:node.children) {
                    if ((m = GameEntityPattern.matcher(child.line)).matches()) {
                        Entity entity = new Entity();
                        entity.EntityID = m.group(1);
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.entityMap.put(entity.EntityID, entity);
                    } else if ((m = PlayerEntityPattern.matcher(child.line)).matches()) {
                        String PlayerID = m.group(2);
                        Entity entity = new Entity();
                        entity.EntityID = m.group(1);
                        entity.PlayerID = PlayerID;
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.entityMap.put(entity.EntityID, entity);
                    }
                }

                GameLogic.gameCreated(mCurrentGame);
            }
        }

        if (mCurrentGame == null) {
            return;
        }

        if ((m = TAG_CHANGE.matcher(line)).matches()) {
            String entityName = m.group(1);
            String key = m.group(2);
            String value = m.group(3);

            Timber.i("TAG_CHANGE " + entityName + " " + key + "=" + value);
            if (!TextUtils.isEmpty(key)) {
                Entity entity = findEntityByName(mCurrentGame, entityName);
                tagChange(entity, key, value);
            }
        } else if ((m = FULL_ENTITY.matcher(line)).matches()) {
            String entityId = decodeEntityName(m.group(1)).get("id");
            Entity entity = mCurrentGame.entityMap.get(entityId);

            boolean isNew = false;
            if (entity == null) {
                entity = new Entity();
                mCurrentGame.entityMap.put(entityId, entity);
                isNew = true;
            }
            entity.EntityID = entityId;
            entity.CardID = m.group(2);
            entity.tags.putAll(getTags(node));

            if (isNew) {
                GameLogic.entityCreated(mCurrentGame, entity);
            }

        } else if ((m = SHOW_ENTITY.matcher(line)).matches()) {
            Entity entity = findEntityByName(mCurrentGame, m.group(1));
            String CardID = m.group(2);
            if (!TextUtils.isEmpty(entity.CardID) && !entity.CardID.equals(CardID)) {
                Timber.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + CardID);
            }
            entity.CardID = CardID;
            entity.card = cardDb.getCard(CardID);

            HashMap<String, String> newTags = getTags(node);
            for (String key:newTags.keySet()) {
                tagChange(entity, key, newTags.get(key));
            }

            GameLogic.entityRevealed(mCurrentGame, entity);
        } else if ((m = HIDE_ENTITY.matcher(line)).matches()) {
            /**
             * do nothing and rely on tag changes instead
             */
        }
    }

    private static Entity findEntityByName(Game game, String name) {
        if (TextUtils.isEmpty(name)) {
            return unknownEntity("empty");
        } else if (name.length() >= 2 && name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']') {
            String id = decodeEntityName(name).get("id");
            if (TextUtils.isEmpty(id)) {
                return unknownEntity(name);
            } else {
                return getEntitySafe(game, id);
            }
        } else if ("GameEntity".equals(name)) {
            return getEntitySafe(game, Entity.ENTITY_ID_GAME);
        } else {
            // this must be a battleTag
            Entity entity = game.entityMap.get(name);
            if (entity == null) {
                Timber.w("Adding battleTag " + name);
                if (game.battleTags.size() >= 2) {
                    Timber.e("[Inconsistent] too many battleTags");
                }
                game.battleTags.add(name);

                entity = new Entity();
                entity.EntityID = name;
                game.entityMap.put(name, entity);
            }
            return entity;
        }
    }

    private static HashMap<String,String> decodeEntityName(String name) {
        return decodeParams(name.substring(1, name.length() - 1));
    }

    private static Entity getEntitySafe(Game game, String entityId) {
        Entity entity = game.entityMap.get(entityId);

        if (entity == null){
            /**
             * do not crash...
             */
            return unknownEntity(entityId);
        }
        return entity;
    }

    private static Entity unknownEntity(String entityId) {
        Timber.e("unknown entity " + entityId);
        Entity entity = new Entity();
        entity.EntityID = entityId;
        return entity;
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
}
