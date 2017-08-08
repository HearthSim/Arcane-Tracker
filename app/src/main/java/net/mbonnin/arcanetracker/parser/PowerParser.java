package net.mbonnin.arcanetracker.parser;

import android.os.Handler;
import android.text.TextUtils;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.hsreplay.HSReplay;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class PowerParser implements LogReader.LineConsumer {
    private final Handler mHandler;
    private final GameLogic mGameLogic;
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

    private StringBuilder rawBuilder;
    private String rawMatchStart;
    private int rawGoldRewardStateCount;
    private Game mLastGame;
    private boolean mReadingPreviousData = true;

    static class Block {
        public static final String TYPE_PLAY = "PLAY";
        public static final String TYPE_POWER = "POWER";
        public static final String TYPE_TRIGGER = "TRIGGER";
        public static final String TYPE_ATTACK = "ATTACK";

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

    public PowerParser() {
        mHandler = new Handler();
        mGameLogic = GameLogic.get();
    }


    public void onLine(String rawLine) {
        Timber.v(rawLine);

        LogReader.LogLine logLine = LogReader.parseLine(rawLine);
        if (logLine == null) {
            return;
        }

        String line = logLine.line;
        if (mReadingPreviousData) {
            return;
        }

        if (logLine.method.startsWith("GameState")) {
            handleRawLine(rawLine);
            return;
        } else if (!"PowerTaskList.DebugPrintPower()".equals(logLine.method)) {
            //Timber.e("Ignore method: " + s[0]);
            return;
        }

        int spaces = 0;
        while (spaces < line.length() && line.charAt(spaces) == ' ') {
            spaces++;
        }

        if (spaces == line.length()) {
            Timber.e("empty line: " + line);
            return;
        } else if (spaces % 4 != 0) {
            Timber.e("bad indentation: " + line);
            return;
        }

        line = line.substring(spaces).trim();

        int depth = spaces / 4;

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
            /*
             * BLOCK_END is a special case :-/
             */
            outputCurrentNode();
            mCurrentNode = node;
        } else {
            parent.children.add(node);
        }

        mNodeStack.add(node);
    }

    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
    }

    private void handleRawLine(String rawLine) {
        if (rawLine.contains("CREATE_GAME")) {
            rawBuilder = new StringBuilder();
            rawMatchStart = Utils.ISO8601DATEFORMAT.format(new Date());

            Timber.w(rawMatchStart + " - CREATE GAME: " + rawLine);
            rawGoldRewardStateCount = 0;
        }

        if (rawBuilder == null) {
            return;
        }

        rawBuilder.append(rawLine);
        rawBuilder.append('\n');

        if (rawLine.contains("GOLD_REWARD_STATE")) {
            rawGoldRewardStateCount++;
            if (rawGoldRewardStateCount == 2) {
                String gameStr = rawBuilder.toString();
                Timber.w("GOLD_REWARD_STATE finished");

                long start = System.currentTimeMillis();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (mCurrentGame != null) {
                            // we are still parsing the PowerState logs, wait
                            if (System.currentTimeMillis() - start < 30000) {
                                mHandler.post(this);
                            } else {
                                Timber.e("timeout waiting for PowerState to finish");
                            }
                        } else {
                            HSReplay.get().uploadGame(rawMatchStart, mLastGame, gameStr);
                        }
                    }
                };

                runnable.run();
                rawBuilder = null;
            }
        }

    }


    HashMap<String, String> getTags(Node node) {
        HashMap<String, String> map = new HashMap<>();

        for (Node child : node.children) {
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

                for (Node child : node.children) {
                    outputAction(block, child);
                }

                if (mCurrentGame != null && Block.TYPE_PLAY.equals(block.blockType)) {
                    Entity entity = findEntityByName(mCurrentGame, m.group(2));
                    mGameLogic.entityPlayed(entity);
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
        return game.player != null && game.opponent != null;
    }

    private void tagChange(Entity entity, String key, String newValue) {
        String oldValue = entity.tags.get(Entity.KEY_ZONE);
        entity.tags.put(key, newValue);

        mGameLogic.tagChanged(entity, key, oldValue, newValue);

        /*
         * Do not crash If we get a disconnect before the mulligan (might happen ?)
         */
        if (Entity.ENTITY_ID_GAME.equals(entity.EntityID) && mCurrentGame.isStarted()) {
            if (Entity.KEY_STEP.equals(key)) {
                if (Entity.STEP_FINAL_GAMEOVER.equals(newValue)) {
                    boolean victory = Entity.PLAYSTATE_WON.equals(mCurrentGame.player.entity.tags.get(Entity.KEY_PLAYSTATE));
                    mCurrentGame.victory = victory;
                    mGameLogic.gameOver();

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
                mCurrentGame = new Game();
                mLastGame = mCurrentGame;
                for (Node child : node.children) {
                    if ((m = GameEntityPattern.matcher(child.line)).matches()) {
                        /**
                         * the game entity
                         */
                        Entity entity = new Entity();
                        entity.EntityID = m.group(1);
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.entityMap.put(entity.EntityID, entity);
                    } else if ((m = PlayerEntityPattern.matcher(child.line)).matches()) {
                        Entity entity = new Entity();
                        entity.EntityID = m.group(1);
                        entity.PlayerID = m.group(2);
                        entity.tags.putAll(getTags(child));

                        mCurrentGame.entityMap.put(entity.EntityID, entity);
                    }
                }

                mGameLogic.gameCreated(mCurrentGame);
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


                /*
                 * detect if the hero or a minion was attacked for the secret detector
                 */
                try {
                    if (block != null && Block.TYPE_ATTACK.equals(block.blockType)
                            && key.equals((Entity.KEY_DEFENDING))
                            && value.equals("1")) {
                        String opponentPlayerId = mCurrentGame.getOpponent().entity.PlayerID;
                        if (entity.CardID != null) {
                            Card card = CardDb.getCard(entity.CardID);

                            EntityList opponentSecretEntityList = mCurrentGame.getEntityList(e -> opponentPlayerId.equals(e.tags.get(Entity.KEY_CONTROLLER)));
                            if (opponentPlayerId.equals(entity.tags.get(Entity.KEY_CONTROLLER))) {
                                for (Entity e2 : opponentSecretEntityList) {
                                    if (Card.TYPE_HERO.equals(card.type)) {
                                        e2.extra.opponentHeroWasAttacked = true;
                                    } else {
                                        e2.extra.minonWasAttacked = true;
                                    }
                                }
                            } else {
                                for (Entity e2 : opponentSecretEntityList) {
                                    if (Card.TYPE_HERO.equals(card.type)) {
                                        e2.extra.playerHeroWasAttacked = true;
                                    }
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
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
            if (!TextUtils.isEmpty(entity.CardID)) {
                entity.card = CardDb.getCard(entity.CardID);
            }
            entity.tags.putAll(getTags(node));

            if (TextUtils.isEmpty(entity.CardID) && block != null) {
                /*
                 * this entity is created by something, try to guess
                 */
                tryToGuessCardIdFromBlock(entity, block);
            }

            if (isNew) {
                mGameLogic.entityCreated(mCurrentGame, entity);
            }

        } else if ((m = SHOW_ENTITY.matcher(line)).matches()) {
            Entity entity = findEntityByName(mCurrentGame, m.group(1));
            String CardID = m.group(2);
            if (!TextUtils.isEmpty(entity.CardID) && !entity.CardID.equals(CardID)) {
                Timber.e("[Inconsistent] entity " + entity + " changed cardId " + entity.CardID + " -> " + CardID);
            }
            entity.CardID = CardID;
            entity.card = CardDb.getCard(CardID);

            HashMap<String, String> newTags = getTags(node);
            for (String key : newTags.keySet()) {
                tagChange(entity, key, newTags.get(key));
            }

            mGameLogic.entityRevealed(entity);
        } else if ((m = HIDE_ENTITY.matcher(line)).matches()) {
            /**
             * do nothing and rely on tag changes instead
             */
        }
    }

    private void tryToGuessCardIdFromBlock(Entity entity, Block block) {
        Entity e = findEntityByName(mCurrentGame, block.entity);
        String actionStartingCardId = e.CardID;

        if (TextUtils.isEmpty(actionStartingCardId)) {
            return;
        }

        String guessedId = null;

        if (Block.TYPE_POWER.equals(block.blockType)) {

            switch (actionStartingCardId) {
                case Card.ID_GANG_UP:
                case Card.ID_RECYCLE:
                case Card.SHADOWCASTER:
                case Card.MANIC_SOULCASTER:
                    guessedId = getTargetId(block);
                    break;
                case Card.ID_BENEATH_THE_GROUNDS:
                    guessedId = Card.ID_AMBUSHTOKEN;
                    break;
                case Card.ID_IRON_JUGGERNAUT:
                    guessedId = Card.ID_BURROWING_MINE_TOKEN;
                    break;
                case Card.FORGOTTEN_TORCH:
                    guessedId = Card.ROARING_TORCH;
                    break;
                case Card.CURSE_OF_RAFAAM:
                    guessedId = Card.CURSED;
                    break;
                case Card.ANCIENT_SHADE:
                    guessedId = Card.ANCIENT_CURSE;
                    break;
                case Card.EXCAVATED_EVIL:
                    guessedId = Card.EXCAVATED_EVIL;
                    break;
                case Card.ELISE:
                    guessedId = Card.MAP_TO_THE_GOLDEN_MONKEY;
                    break;
                case Card.MAP_TO_THE_GOLDEN_MONKEY:
                    guessedId = Card.GOLDEN_MONKEY;
                    break;
                case Card.DOOMCALLER:
                    guessedId = Card.CTHUN;
                    break;
                case Card.JADE_IDOL:
                    guessedId = Card.JADE_IDOL;
                    break;
                case Card.FLAME_GEYSER:
                case Card.FIREFLY:
                    guessedId = Card.FLAME_ELEMENTAL;
                    break;
                case Card.STEAM_SURGER:
                    guessedId = Card.FLAME_GEYSER;
                    break;
                case Card.RAZORPETAL_VOLLEY:
                case Card.RAZORPETAL_LASHER:
                    guessedId = Card.RAZORPETAL;
                    break;
                case Card.BURGLY_BULLY:
                    guessedId = Card.ID_COIN;
                    break;
                case Card.MUKLA_TYRANT:
                case Card.KING_MUKLA:
                    guessedId = Card.BANANA;
                    break;
                case Card.JUNGLE_GIANTS:
                    guessedId = Card.BARNABUS;
                    break;
                case Card.THE_MARSH_QUEEN:
                    guessedId = Card.QUEEN_CARNASSA;
                    break;
                case Card.OPEN_THE_WAYGATE:
                    guessedId = Card.TIME_WARP;
                    break;
                case Card.THE_LAST_KALEIDOSAUR:
                    guessedId = Card.GALVADON;
                    break;
                case Card.AWAKEN_THE_MAKERS:
                    guessedId = Card.AMARA;
                    break;
                case Card.CAVERNS_BELOW:
                    guessedId = Card.CRYSTAL_CORE;
                    break;
                case Card.UNITE_THE_MURLOCS:
                    guessedId = Card.MEGAFIN;
                    break;
                case Card.LAKKARI_SACRIFICE:
                    guessedId = Card.NETHER_PORTAL;
                    break;
                case Card.FIRE_PLUME:
                    guessedId = Card.SULFURAS;
                    break;
            }
        } else if (Block.TYPE_TRIGGER.equals(block.blockType)) {
            switch (actionStartingCardId) {
                case Card.PYROS2:
                    guessedId = Card.PYROS6;
                    break;
                case Card.PYROS6:
                    guessedId = Card.PYROS10;
                    break;
                case Card.WHITE_EYES:
                    guessedId = Card.STORM_GUARDIAN;
                    break;
                case Card.DEADLY_FORK:
                    guessedId = Card.SHARP_FORK;
                    break;
                case Card.IGNEOUS_ELEMENTAL:
                    guessedId = Card.FLAME_ELEMENTAL;
                    break;
                case Card.RHONIN:
                    guessedId = Card.ARCANE_MISSILE;
                    break;
            }
        }
        if (guessedId != null) {
            entity.CardID = guessedId;
            entity.card = CardDb.getCard(guessedId);
            entity.extra.createdBy = guessedId;
        }
    }

    private String getTargetId(Block block) {
        Entity entity = findEntityByName(mCurrentGame, block.target);
        return entity.CardID;
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

    private static HashMap<String, String> decodeEntityName(String name) {
        return decodeParams(name.substring(1, name.length() - 1));
    }

    private static Entity getEntitySafe(Game game, String entityId) {
        Entity entity = game.entityMap.get(entityId);

        if (entity == null) {
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
