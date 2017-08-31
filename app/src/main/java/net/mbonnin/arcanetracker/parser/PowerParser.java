package net.mbonnin.arcanetracker.parser;

import com.annimon.stream.function.Consumer;

import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.power.BlockTag;
import net.mbonnin.arcanetracker.parser.power.CreateGameTag;
import net.mbonnin.arcanetracker.parser.power.FullEntityTag;
import net.mbonnin.arcanetracker.parser.power.GameEntityTag;
import net.mbonnin.arcanetracker.parser.power.HideEntityTag;
import net.mbonnin.arcanetracker.parser.power.MetaDataTag;
import net.mbonnin.arcanetracker.parser.power.PlayerTag;
import net.mbonnin.arcanetracker.parser.power.ShowEntityTag;
import net.mbonnin.arcanetracker.parser.power.Tag;
import net.mbonnin.arcanetracker.parser.power.TagChangeTag;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

    private final Pattern BLOCK_START_PATTERN = Pattern.compile("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*) SubOption=(.*)");
    private final Pattern BLOCK_START_CONTINUATION_PATTERN = Pattern.compile("(.*) TriggerKeyword=(.*)");
    private final Pattern BLOCK_END_PATTERN = Pattern.compile("BLOCK_END");

    private final Pattern GameEntityPattern = Pattern.compile("GameEntity EntityID=(.*)");
    private final Pattern PlayerEntityPattern = Pattern.compile("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)");

    private final Pattern FULL_ENTITY = Pattern.compile("FULL_ENTITY - Updating (.*) CardID=(.*)");
    private final Pattern TAG_CHANGE = Pattern.compile("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern SHOW_ENTITY = Pattern.compile("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)");

    private final Pattern HIDE_ENTITY = Pattern.compile("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)");
    private final Pattern TAG = Pattern.compile("tag=(.*) value=(.*)");
    private final Pattern META_DATA = Pattern.compile("META_DATA - Meta=(.*) Data=(.*) Info=(.*)");
    private final Pattern INFO = Pattern.compile("Info\\[[0-9]*\\] = (.*)");

    private StringBuilder rawBuilder;
    private String rawMatchStart;
    private int rawGoldRewardStateCount;
    private boolean mReadingPreviousData = true;
    private ArrayList<BlockTag> mBlockTagStack = new ArrayList<>();
    private Tag mCurrentTag;

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

        if (logLine.method.startsWith("GameState")) {
            handleGameStateLine(rawLine);
        } else if (logLine.method.startsWith("PowerTaskList.DebugPrintPower()")) {

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

            Matcher m;
            Tag newTag = null;

            if (("CREATE_GAME".equals(line))) {
                /*
                 * reset any previous state
                 */
                mCurrentTag = null;
                mBlockTagStack.clear();

                newTag = new CreateGameTag();

            } else if ((m = FULL_ENTITY.matcher(line)).matches()) {
                FullEntityTag tag = new FullEntityTag();
                tag.ID = getEntityIdFromNameOrId(m.group(1));
                tag.CardID = m.group(2);

                newTag = tag;
            } else if ((m = TAG_CHANGE.matcher(line)).matches()) {
                TagChangeTag tag = new TagChangeTag();
                tag.ID = getEntityIdFromNameOrId(m.group(1));
                tag.tag = m.group(2);
                tag.value = m.group(3);

                newTag = tag;
            } else if ((m = SHOW_ENTITY.matcher(line)).matches()) {
                ShowEntityTag tag = new ShowEntityTag();
                tag.Entity = getEntityIdFromNameOrId(m.group(1));
                tag.CardID = m.group(2);

                newTag = tag;
            } else if ((m = HIDE_ENTITY.matcher(line)).matches()) {
                HideEntityTag tag = new HideEntityTag();
                tag.Entity = getEntityIdFromNameOrId(m.group(1));
                tag.tag = m.group(2);
                tag.value = m.group(3);

                newTag = tag;
            } else if ((m = META_DATA.matcher(line)).matches()) {
                MetaDataTag tag = new MetaDataTag();
                tag.Meta = m.group(1);
                tag.Data = m.group(2);

                newTag = tag;
            }

            if (newTag != null ) {
                openNewTag(newTag);
                return;
            }

            if ((m = BLOCK_START_PATTERN.matcher(line)).matches()) {
                BlockTag tag = new BlockTag();
                tag.BlockType = m.group(1);
                tag.Entity = getEntityIdFromNameOrId(m.group(2));
                tag.EffectCardId = m.group(3);
                tag.EffectIndex = m.group(4);
                tag.Target = getEntityIdFromNameOrId(m.group(5));
                tag.SubOption = m.group(6);
                if ((m = BLOCK_START_CONTINUATION_PATTERN.matcher(m.group(6))).matches()) {
                    tag.SubOption = m.group(1);
                    tag.TriggerKeyword = m.group(2);
                }

                openNewTag(null);

                if (mBlockTagStack.size() > 0) {
                    mBlockTagStack.get(mBlockTagStack.size() - 1).children.add(tag);
                }
                mBlockTagStack.add(tag);
                return;
            } else if ((m = BLOCK_END_PATTERN.matcher(line)).matches()) {
                openNewTag(null);
                if (mBlockTagStack.size() > 0) {
                    BlockTag blockTag = mBlockTagStack.remove(mBlockTagStack.size() - 1);
                    if (mBlockTagStack.size() == 0) {
                        mTagConsumer.accept(blockTag);
                    }
                } else {
                    Timber.e("BLOCK_END without BLOCK_START");
                }
                return;
            }


            if ((m = GameEntityPattern.matcher(line)).matches()) {
                GameEntityTag tag = new GameEntityTag();
                tag.EntityID = getEntityIdFromNameOrId(m.group(1));

                if (mCurrentTag instanceof CreateGameTag) {
                    ((CreateGameTag) mCurrentTag).gameEntity = tag;
                }
            } else if ((m = PlayerEntityPattern.matcher(line)).matches()) {
                PlayerTag tag = new PlayerTag();
                tag.EntityID = getEntityIdFromNameOrId(m.group(1));
                tag.PlayerID = m.group(2);

                if (mCurrentTag instanceof CreateGameTag) {
                    ((CreateGameTag) mCurrentTag).playerList.add(tag);
                }
            } else if ((m = TAG.matcher(line)).matches()) {
                String key = m.group(1);
                String value = m.group(2);

                if (mCurrentTag instanceof CreateGameTag) {
                    if (((CreateGameTag) mCurrentTag).playerList.size() > 0) {
                        ((CreateGameTag) mCurrentTag).playerList.get(((CreateGameTag) mCurrentTag).playerList.size() - 1).tags.put(key, value);
                    } else if (((CreateGameTag) mCurrentTag).gameEntity != null) {
                        ((CreateGameTag) mCurrentTag).gameEntity.tags.put(key, value);
                    } else {
                        Timber.e("wrong tag=");
                    }
                } else if (mCurrentTag instanceof ShowEntityTag) {
                    ((ShowEntityTag) mCurrentTag).tags.put(key, value);
                } else if (mCurrentTag instanceof FullEntityTag) {
                    ((FullEntityTag) mCurrentTag).tags.put(key, value);
                } else {
                    Timber.e("got tag= outside of valid tag");
                }
            } else if ((m = INFO.matcher(line)).matches()) {
                if (mCurrentTag instanceof MetaDataTag) {
                    ((MetaDataTag) mCurrentTag).Info.add(getEntityIdFromNameOrId(m.group(1)));
                }
            }
        }
    }

    private void openNewTag(Tag newTag) {
        if (mCurrentTag != null) {
            if (mBlockTagStack.size() > 0) {
                mBlockTagStack.get(mBlockTagStack.size() - 1).children.add(mCurrentTag);
            } else {
                mTagConsumer.accept(mCurrentTag);
            }
        }
        mCurrentTag = newTag;
    }

    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
    }

    private void handleGameStateLine(String rawLine) {
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
