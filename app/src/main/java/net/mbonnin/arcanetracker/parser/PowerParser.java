package net.mbonnin.arcanetracker.parser;

import com.annimon.stream.function.Consumer;

import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.power.BlockTag;
import net.mbonnin.arcanetracker.parser.power.CreateGameTag;
import net.mbonnin.arcanetracker.parser.power.FullEntityTag;
import net.mbonnin.arcanetracker.parser.power.GameEntityTag;
import net.mbonnin.arcanetracker.parser.power.HideEntityTag;
import net.mbonnin.arcanetracker.parser.power.PlayerTag;
import net.mbonnin.arcanetracker.parser.power.ShowEntityTag;
import net.mbonnin.arcanetracker.parser.power.Tag;
import net.mbonnin.arcanetracker.parser.power.TagChangeTag;
import net.mbonnin.arcanetracker.parser.power.UnknownTag;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.functions.Func2;
import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class PowerParser implements LogReader.LineConsumer {
    private final Consumer<Tag> mTagConsumer;
    private final Func2<String, String, Void> mRawGameConsumer;
    private LinkedList<Node> mNodeStack = new LinkedList<Node>();
    private Node mCurrentRoot;

    private final Pattern BLOCK_START_PATTERN = Pattern.compile("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*)");
    private final Pattern BLOCK_END_PATTERN = Pattern.compile("BLOCK_END");

    private final Pattern GameEntityPattern = Pattern.compile("GameEntity EntityID=(.*)");
    private final Pattern PlayerEntityPattern = Pattern.compile("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)");

    private final Pattern FULL_ENTITY = Pattern.compile("FULL_ENTITY - Creating ID=(.*) CardID=(.*)");
    private final Pattern TAG_CHANGE = Pattern.compile("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern SHOW_ENTITY = Pattern.compile("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)");

    private final Pattern HIDE_ENTITY = Pattern.compile("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG = Pattern.compile("tag=(.*) value=(.*)");

    private StringBuilder rawBuilder;
    private String rawMatchStart;
    private int rawGoldRewardStateCount;
    private Game mLastGame;
    private boolean mReadingPreviousData = true;

    static class Node {
        String line;
        ArrayList<Node> children = new ArrayList<>();
        public int depth;
    }

    public PowerParser(Consumer<Tag> tagConsumer, Func2<String, String, Void> rawGameConsumer) {
        mTagConsumer = tagConsumer;
        mRawGameConsumer = rawGameConsumer;
    }


    public void onLine(String rawLine) {
        LogReader.LogLine logLine = LogReader.parseLine(rawLine);
        if (logLine == null) {
            return;
        }

        String line = logLine.line;
        if (mReadingPreviousData) {
            return;
        }

        if (!logLine.method.startsWith("GameState"))
            return;

        handleRawLine(rawLine);

        if (!logLine.method.equals("GameState.DebugPrintPower()")){
            return;
        }

        Timber.v(rawLine);

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

        if ("BLOCK_END".equals(line)) {
            // just ignore the BLOCK_END stuff, we'll just base our parsing on indentation
            return;
        }

        int depth = spaces / 4;

        Node node = new Node();
        node.depth = depth;
        node.line = line;

        Node parent = null;

        /*
         * lookup the parent this node belongs to
         */
        while (!mNodeStack.isEmpty()) {
            Node node2 = mNodeStack.peekLast();
            if (depth == node2.depth + 1) {
                parent = node2;
                break;
            }
            mNodeStack.removeLast();
        }
        if (parent == null) {
            if (node.depth > 0) {
                Timber.e("orphan node");
                return;
            }
            if (mCurrentRoot != null) {
                outputCurrentRoot(mCurrentRoot);
            }
            mCurrentRoot = node;
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

                if (mRawGameConsumer != null) {
                    mRawGameConsumer.call(gameStr, rawMatchStart);
                }

                rawBuilder = null;
            }
        }

    }


    private HashMap<String, String> getTags(Node node) {
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


    private void outputCurrentRoot(Node node) {
        //dumpNode(node);

        try {
            Tag tag = parseTag(node);
            mTagConsumer.accept(tag);
        } catch (Exception e) {
            Timber.e("cannot parse tag");
            Timber.e(e);
        }
    }

    private Tag parseTag(Node node) {
        Matcher m;
        String line = node.line;

        if (("CREATE_GAME".equals(line))) {
            CreateGameTag tag = new CreateGameTag();

            tag.gameEntity = (GameEntityTag) parseTag(node.children.get(0));
            tag.player1 = (PlayerTag) parseTag(node.children.get(1));
            tag.player2 = (PlayerTag) parseTag(node.children.get(2));

            return tag;
        } else if ((m = GameEntityPattern.matcher(line)).matches()) {
            GameEntityTag tag = new GameEntityTag();
            tag.EntityID = getEntityIdFromNameOrId(m.group(1));
            tag.tags.putAll(getTags(node));

            return tag;
        } else if ((m = PlayerEntityPattern.matcher(line)).matches()) {
            PlayerTag tag = new PlayerTag();
            tag.EntityID = getEntityIdFromNameOrId(m.group(1));
            tag.PlayerID = m.group(2);
            tag.tags.putAll(getTags(node));

            return tag;
        } else if ((m = FULL_ENTITY.matcher(line)).matches()) {
            FullEntityTag tag = new FullEntityTag();
            tag.ID = getEntityIdFromNameOrId(m.group(1));
            tag.CardID = m.group(2);
            tag.tags.putAll(getTags(node));

            return tag;
        } else if ((m = TAG_CHANGE.matcher(line)).matches()) {
            TagChangeTag tag = new TagChangeTag();
            tag.ID = getEntityIdFromNameOrId(m.group(1));
            tag.tag = m.group(2);
            tag.value = m.group(3);

            return tag;
        } else if ((m = BLOCK_START_PATTERN.matcher(line)).matches()) {
            BlockTag tag = new BlockTag();
            tag.BlockType = m.group(1);
            tag.Entity = getEntityIdFromNameOrId(m.group(2));
            tag.EffectCardId = m.group(3);
            tag.EffectIndex = m.group(4);
            tag.Target = getEntityIdFromNameOrId(m.group(5));

            for (Node child : node.children) {
                tag.children.add(parseTag(child));
            }

            return tag;
        } else if ((m = SHOW_ENTITY.matcher(line)).matches()) {
            ShowEntityTag tag = new ShowEntityTag();
            tag.Entity = getEntityIdFromNameOrId(m.group(1));
            tag.CardID = m.group(2);
            tag.tags.putAll(getTags(node));

            return tag;
        } else if ((m = HIDE_ENTITY.matcher(line)).matches()) {
            HideEntityTag tag = new HideEntityTag();
            tag.Entity = getEntityIdFromNameOrId(m.group(1));
            tag.tag = m.group(2);
            tag.value = m.group(3);
            
            return tag;
        } else {
            Timber.e("unknown tag: " + line);
            return new UnknownTag();
        }
    }

    private void dumpNode(Node node) {
        Timber.v(node.line);
        for (Node child: node.children) {
            dumpNode(child);
        }
    }

    private static String getEntityIdFromNameOrId(String nameOrId) {
        if (nameOrId.length() >= 2 && nameOrId.charAt(0) == '[' && nameOrId.charAt(nameOrId.length() - 1) == ']') {
            return decodeEntityName(nameOrId).get("id");
        } else {
            return nameOrId;
        }
    }

    private static HashMap<String, String> decodeEntityName(String name) {
        return decodeParams(name.substring(1, name.length() - 1));
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
