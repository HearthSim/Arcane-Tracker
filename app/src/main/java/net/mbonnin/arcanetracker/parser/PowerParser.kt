package net.mbonnin.arcanetracker.parser

import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.parser.power.*
import timber.log.Timber
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by martin on 10/27/16.
 */

class PowerParser(private val mTagConsumer: (Tag) -> Unit, private val mRawGameConsumer: (String, String) -> Unit) : LogReader.LineConsumer {

    private val BLOCK_START_PATTERN = Pattern.compile("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*) SubOption=(.*)")
    private val BLOCK_START_CONTINUATION_PATTERN = Pattern.compile("(.*) TriggerKeyword=(.*)")
    private val BLOCK_END_PATTERN = Pattern.compile("BLOCK_END")

    private val GameEntityPattern = Pattern.compile("GameEntity EntityID=(.*)")
    private val PlayerEntityPattern = Pattern.compile("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)")

    private val FULL_ENTITY = Pattern.compile("FULL_ENTITY - Creating ID=(.*) CardID=(.*)")
    private val TAG_CHANGE = Pattern.compile("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)")
    private val SHOW_ENTITY = Pattern.compile("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)")

    private val HIDE_ENTITY = Pattern.compile("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)")
    private val TAG = Pattern.compile("tag=(.*) value=(.*)")
    private val META_DATA = Pattern.compile("META_DATA - Meta=(.*) Data=(.*) Info=(.*)")
    private val INFO = Pattern.compile("Info\\[[0-9]*\\] = (.*)")

    private val BUILD_NUMBER = Pattern.compile("BuildNumber=(.*)")
    private val GAME_TYPE = Pattern.compile("GameType=(.*)")
    private val FORMAT_TYPE = Pattern.compile("FormatType=(.*)")
    private val SCENARIO_ID = Pattern.compile("ScenarioID=(.*)")
    private val PLAYER_MAPPING = Pattern.compile("PlayerID=(.*), PlayerName=(.*)")

    private var rawBuilder: StringBuilder? = null
    private var rawMatchStart: String? = null
    private var rawGoldRewardStateCount: Int = 0
    private var mReadingPreviousData = true
    private val mBlockTagStack = ArrayList<BlockTag>()
    private var mCurrentTag: Tag? = null


    override fun onLine(rawLine: String) {
        if (rawLine.startsWith("================== Begin Spectating")) {
            mTagConsumer(SpectatorTag(true))
            return
        } else if (rawLine.startsWith("================== End Spectator Mode")) {
            mTagConsumer(SpectatorTag(false))
            return
        }

        val logLine = LogReader.parseLineWithMethod(rawLine) ?: return

        val line = logLine.line.trim()
        if (mReadingPreviousData) {
            return
        }

        if (!logLine.method!!.startsWith("GameState")) {
            return
        }

        Timber.v(rawLine)

        if (logLine.method!!.startsWith("GameState.DebugPrintGame()")) {
            handleDebugPrintGame(line)
        } else if (logLine.method!!.startsWith("GameState.DebugPrintPower()")) {
            handleDebugPrintPower(line)
        }

        if (rawBuilder != null) {
            rawBuilder!!.append(rawLine)
            rawBuilder!!.append('\n')

            if (rawLine.contains("GOLD_REWARD_STATE")) {
                rawGoldRewardStateCount++
                if (rawGoldRewardStateCount == 2) {
                    val gameStr = rawBuilder!!.toString()
                    Timber.w("GOLD_REWARD_STATE finished")

                    mRawGameConsumer(gameStr, rawMatchStart!!)

                    rawBuilder = null
                }
            }
        }
    }

    private fun handleDebugPrintPower(line: String) {
        var m: Matcher
        var newTag: Tag? = null

        if ("TAG_CHANGE Entity=GameEntity tag=STEP value=FINAL_GAMEOVER" == line) {
            /*
             *  it could happen that the game is stopped in the middle of a block
             */
            if (mBlockTagStack.size > 0) {
                Timber.d("Ended in the middle of a block")
                if (mCurrentTag != null) {
                    mBlockTagStack[mBlockTagStack.size - 1].children.add(mCurrentTag!!)
                }
                mTagConsumer(mBlockTagStack[0])
                mBlockTagStack.clear()
                mCurrentTag = null
            }
        }

        tagLoop@ while (true) {
            if ("CREATE_GAME" == line) {
                /*
                 * reset any previous state in case there are 2 CREATE_GAME in a row
                 */
                mCurrentTag = null
                mBlockTagStack.clear()

                rawBuilder = StringBuilder()
                rawMatchStart = Utils.ISO8601DATEFORMAT.format(Date())

                rawGoldRewardStateCount = 0


                newTag = CreateGameTag()
                break@tagLoop
            }

            m = FULL_ENTITY.matcher(line)
            if (m.matches()) {
                val tag = FullEntityTag()
                tag.ID = getEntityIdFromNameOrId(m.group(1))
                tag.CardID = m.group(2)

                newTag = tag
                break@tagLoop
            }

            m = TAG_CHANGE.matcher(line)
            if (m.matches()) {
                val tag = TagChangeTag()
                tag.ID = getEntityIdFromNameOrId(m.group(1))
                tag.tag = m.group(2)
                tag.value = m.group(3)

                newTag = tag
                break@tagLoop
            }

            m = SHOW_ENTITY.matcher(line)
            if (m.matches()) {
                val tag = ShowEntityTag()
                tag.Entity = getEntityIdFromNameOrId(m.group(1))
                tag.CardID = m.group(2)

                newTag = tag
                break@tagLoop
            }

            m = HIDE_ENTITY.matcher(line)
            if (m.matches()) {
                val tag = HideEntityTag()
                tag.Entity = getEntityIdFromNameOrId(m.group(1))
                tag.tag = m.group(2)
                tag.value = m.group(3)

                newTag = tag
                break@tagLoop
            }

            m = META_DATA.matcher(line)
            if (m.matches()) {
                val tag = MetaDataTag()
                tag.Meta = m.group(1)
                tag.Data = m.group(2)

                newTag = tag
                break@tagLoop
            }

            break@tagLoop
        }


        if (newTag != null) {
            openNewTag(newTag)
            return
        }

        m = BLOCK_START_PATTERN.matcher(line)
        if (m.matches()) {
            val m2 = BLOCK_START_CONTINUATION_PATTERN.matcher(m.group(6))
            val subOption: String?
            val triggerKeyWord: String?
            if (m2.matches()) {
                subOption = m2.group(1)
                triggerKeyWord = m2.group(2)
            } else {
                subOption = m.group(6)
                triggerKeyWord = null
            }
            val tag = BlockTag(
                    BlockType = m.group(1),
                    Entity = getEntityIdFromNameOrId(m.group(2)),
                    EffectCardId = m.group(3),
                    EffectIndex = m.group(4),
                    Target = getEntityIdFromNameOrId(m.group(5)),
                    SubOption = subOption,
                    TriggerKeyword = triggerKeyWord,
                    children = mutableListOf()
            )

            openNewTag(null)

            if (mBlockTagStack.size > 0) {
                mBlockTagStack[mBlockTagStack.size - 1].children.add(tag)
            }
            mBlockTagStack.add(tag)
            return
        }

        m = BLOCK_END_PATTERN.matcher(line)
        if (m.matches()) {
            openNewTag(null)
            if (mBlockTagStack.size > 0) {
                val blockTag = mBlockTagStack.removeAt(mBlockTagStack.size - 1)
                if (mBlockTagStack.size == 0) {
                    mTagConsumer(blockTag)
                }
            } else {
                Timber.e("BLOCK_END without BLOCK_START")
            }
            return
        }


        contentLoop@ while (true) {
            m = GameEntityPattern.matcher(line)
            if (m.matches()) {
                val tag = GameEntityTag()
                tag.EntityID = getEntityIdFromNameOrId(m.group(1))

                if (mCurrentTag is CreateGameTag) {
                    (mCurrentTag as CreateGameTag).gameEntity = tag
                }
                break@contentLoop
            }

            m = PlayerEntityPattern.matcher(line)
            if (m.matches()) {
                val tag = PlayerTag()
                tag.EntityID = getEntityIdFromNameOrId(m.group(1))
                tag.PlayerID = m.group(2)

                if (mCurrentTag is CreateGameTag) {
                    (mCurrentTag as CreateGameTag).playerList.add(tag)
                }
                break@contentLoop
            }

            m = TAG.matcher(line)
            if (m.matches()) {
                val key = m.group(1)
                val value = m.group(2)

                if (mCurrentTag is CreateGameTag) {
                    if ((mCurrentTag as CreateGameTag).playerList.size > 0) {
                        (mCurrentTag as CreateGameTag).playerList[(mCurrentTag as CreateGameTag).playerList.size - 1].tags[key] = value
                    } else if ((mCurrentTag as CreateGameTag).gameEntity != null) {
                        (mCurrentTag as CreateGameTag).gameEntity.tags[key] = value
                    } else {
                        Timber.e("wrong tag=")
                    }
                } else if (mCurrentTag is ShowEntityTag) {
                    (mCurrentTag as ShowEntityTag).tags[key] = value
                } else if (mCurrentTag is FullEntityTag) {
                    (mCurrentTag as FullEntityTag).tags[key] = value
                } else {
                    Timber.e("got tag= outside of valid tag")
                }
                break@contentLoop
            }

            m = INFO.matcher(line)
            if (m.matches()) {
                if (mCurrentTag is MetaDataTag) {
                    (mCurrentTag as MetaDataTag).Info.add(getEntityIdFromNameOrId(m.group(1)))
                }
                break@contentLoop
            }

            break@contentLoop
        }
    }

    private fun handleDebugPrintGame(line: String) {
        var m: Matcher

        m = BUILD_NUMBER.matcher(line)
        if (m.matches()) {
            mTagConsumer(BuildNumberTag(m.group(1)))
            return
        }

        m = GAME_TYPE.matcher(line)
        if (m.matches()) {
            mTagConsumer(GameTypeTag(m.group(1)))
            return
        }

        m = FORMAT_TYPE.matcher(line)
        if (m.matches()) {
            mTagConsumer(FormatTypeTag(m.group(1)))
            return
        }

        m = SCENARIO_ID.matcher(line)
        if (m.matches()) {
            mTagConsumer(ScenarioIdTag(m.group(1)))
            return
        }

        m = PLAYER_MAPPING.matcher(line)
        if (m.matches()) {
            mTagConsumer(PlayerMappingTag(m.group(1), m.group(2)))
            return
        }
    }


    private fun openNewTag(newTag: Tag?) {
        if (mCurrentTag != null) {
            if (mBlockTagStack.size > 0) {
                mBlockTagStack[mBlockTagStack.size - 1].children.add(mCurrentTag!!)
            } else {
                mTagConsumer(mCurrentTag!!)
            }
        }
        mCurrentTag = newTag
    }

    override fun onPreviousDataRead() {
        mReadingPreviousData = false
    }

    private fun getEntityIdFromNameOrId(nameOrId: String): String? {
        return if (nameOrId.length >= 2 && nameOrId[0] == '[' && nameOrId[nameOrId.length - 1] == ']') {
            decodeEntityName(nameOrId)["id"]
        } else {
            nameOrId
        }
    }

    private fun decodeEntityName(name: String): HashMap<String, String> {
        return decodeParams(name.substring(1, name.length - 1))
    }

    private fun decodeParams(params: String): HashMap<String, String> {
        var end = params.length
        val map = HashMap<String, String>()

        while (true) {
            var start = end - 1

            val value: String
            while (start >= 0 && params[start] != '=') {
                start--
            }
            if (start < 0) {
                return map
            }
            value = params.substring(start + 1, end)
            end = start
            if (end < 0) {
                return map
            }
            start = end - 1
            while (start >= 0 && params[start] != ' ') {
                start--
            }
            val key: String
            if (start == 0) {
                key = params.substring(start, end)
            } else {
                key = params.substring(start + 1, end)
            }
            map[key.trim { it <= ' ' }] = value
            if (start == 0) {
                break
            } else {
                end = start
            }
        }

        return map
    }
}
