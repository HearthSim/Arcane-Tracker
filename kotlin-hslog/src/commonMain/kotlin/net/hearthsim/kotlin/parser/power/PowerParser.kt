package net.hearthsim.kotlin.parser.power

import com.soywiz.klock.DateTime
import net.hearthsim.kotlin.parser.LogLine

/**
 * Created by martin on 10/27/16.
 */

class PowerParser(
        private val mTagConsumer: (Tag) -> Unit,
        private val mRawGameConsumer: ((rawGame: String, unixMillis: Long) -> Unit)?,
        private val logger: ((String, Array<out String>) -> Unit)?
) {

    private val BLOCK_START_PATTERN = Regex("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*) SubOption=(.*)")
    private val BLOCK_START_CONTINUATION_PATTERN = Regex("(.*) TriggerKeyword=(.*)")
    private val BLOCK_END_PATTERN = Regex("BLOCK_END")

    private val GameEntityPattern = Regex("GameEntity EntityID=(.*)")
    private val PlayerEntityPattern = Regex("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)")

    private val FULL_ENTITY = Regex("FULL_ENTITY - Creating ID=(.*) CardID=(.*)")
    private val TAG_CHANGE = Regex("TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)")
    private val SHOW_ENTITY = Regex("SHOW_ENTITY - Updating Entity=(.*) CardID=(.*)")

    private val HIDE_ENTITY = Regex("HIDE_ENTITY - Entity=(.*) tag=(.*) value=(.*)")
    private val TAG = Regex("tag=(.*) value=(.*)")
    private val META_DATA = Regex("META_DATA - Meta=(.*) Data=(.*) Info=(.*)")
    private val INFO = Regex("Info\\[[0-9]*\\] = (.*)")

    private val BUILD_NUMBER = Regex("BuildNumber=(.*)")
    private val GAME_TYPE = Regex("GameType=(.*)")
    private val FORMAT_TYPE = Regex("FormatType=(.*)")
    private val SCENARIO_ID = Regex("ScenarioID=(.*)")
    private val PLAYER_MAPPING = Regex("PlayerID=(.*), PlayerName=(.*)")

    private var rawBuilder: StringBuilder? = null
    private var rawMatchStart: Long? = null
    private var rawGoldRewardStateCount: Int = 0
    private val mBlockTagStack = ArrayList<BlockTag>()
    private var mCurrentTag: Tag? = null

    private fun log(format: String, vararg args: String) {
        logger?.invoke(format, args)
    }

    fun process(rawLine: String, processGameTags: Boolean) {
        if (rawLine.startsWith("================== Begin Spectating")) {
            mTagConsumer(SpectatorTag(true))
            return
        } else if (rawLine.startsWith("================== End Spectator Mode")) {
            mTagConsumer(SpectatorTag(false))
            return
        }

        val logLine = LogLine.parseLineWithMethod(rawLine, logger) ?: return

        val line = logLine.line.trim()
        if (!processGameTags) {
            return
        }

        if (!logLine.method!!.startsWith("GameState")) {
            return
        }

        log(rawLine)

        if (logLine.method.startsWith("GameState.DebugPrintGame()")) {
            handleDebugPrintGame(line)
        } else if (logLine.method.startsWith("GameState.DebugPrintPower()")) {
            handleDebugPrintPower(line)
        }

        if (rawBuilder != null) {
            rawBuilder!!.append(rawLine)
            rawBuilder!!.append('\n')

            if (rawLine.contains("GOLD_REWARD_STATE")) {
                rawGoldRewardStateCount++
                if (rawGoldRewardStateCount == 2) {
                    val gameStr = rawBuilder!!.toString()
                    log("GOLD_REWARD_STATE finished")

                    mRawGameConsumer?.invoke(gameStr, rawMatchStart!!)

                    rawBuilder = null
                }
            }
        }
    }

    private fun handleDebugPrintPower(line: String) {
        var m: MatchResult?
        var newTag: Tag? = null

        if ("TAG_CHANGE Entity=GameEntity tag=STEP value=FINAL_GAMEOVER" == line) {
            /*
             *  it could happen that the game is stopped in the middle of a block
             */
            if (mBlockTagStack.size > 0) {
                log("Ended in the middle of a block")
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
                rawMatchStart = DateTime.now().unixMillisLong

                rawGoldRewardStateCount = 0


                newTag = CreateGameTag()
                break@tagLoop
            }

            m = FULL_ENTITY.matchEntire(line)
            if (m != null) {
                val tag = FullEntityTag()
                tag.ID = getEntityIdFromNameOrId(m.groupValues[1])
                tag.CardID = m.groupValues[2]

                newTag = tag
                break@tagLoop
            }

            m = TAG_CHANGE.matchEntire(line)
            if (m != null) {
                val tag = TagChangeTag()
                tag.ID = getEntityIdFromNameOrId(m.groupValues[1])
                tag.tag = m.groupValues[2]
                tag.value = m.groupValues[3]

                newTag = tag
                break@tagLoop
            }

            m = SHOW_ENTITY.matchEntire(line)
            if (m != null) {
                val tag = ShowEntityTag()
                tag.Entity = getEntityIdFromNameOrId(m.groupValues[1])
                tag.CardID = m.groupValues[2]

                newTag = tag
                break@tagLoop
            }

            m = HIDE_ENTITY.matchEntire(line)
            if (m != null) {
                val tag = HideEntityTag()
                tag.Entity = getEntityIdFromNameOrId(m.groupValues[1])
                tag.tag = m.groupValues[2]
                tag.value = m.groupValues[3]

                newTag = tag
                break@tagLoop
            }

            m = META_DATA.matchEntire(line)
            if (m != null) {
                val tag = MetaDataTag()
                tag.Meta = m.groupValues[1]
                tag.Data = m.groupValues[2]

                newTag = tag
                break@tagLoop
            }

            break@tagLoop
        }


        if (newTag != null) {
            openNewTag(newTag)
            return
        }

        m = BLOCK_START_PATTERN.matchEntire(line)
        if (m != null) {
            val m2 = BLOCK_START_CONTINUATION_PATTERN.matchEntire(m.groupValues[6])
            val subOption: String?
            val triggerKeyWord: String?
            if (m2 != null) {
                subOption = m2.groupValues[1]
                triggerKeyWord = m2.groupValues[2]
            } else {
                subOption = m.groupValues[6]
                triggerKeyWord = null
            }
            val tag = BlockTag(
                    BlockType = m.groupValues[1],
                    Entity = getEntityIdFromNameOrId(m.groupValues[2]),
                    EffectCardId = m.groupValues[3],
                    EffectIndex = m.groupValues[4],
                    Target = getEntityIdFromNameOrId(m.groupValues[5]),
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

        m = BLOCK_END_PATTERN.matchEntire(line)
        if (m != null) {
            openNewTag(null)
            if (mBlockTagStack.size > 0) {
                val blockTag = mBlockTagStack.removeAt(mBlockTagStack.size - 1)
                if (mBlockTagStack.size == 0) {
                    mTagConsumer(blockTag)
                }
            } else {
                log("BLOCK_END without BLOCK_START")
            }
            return
        }


        contentLoop@ while (true) {
            m = GameEntityPattern.matchEntire(line)
            if (m != null) {
                val tag = GameEntityTag()
                tag.EntityID = getEntityIdFromNameOrId(m.groupValues[1])

                if (mCurrentTag is CreateGameTag) {
                    (mCurrentTag as CreateGameTag).gameEntity = tag
                }
                break@contentLoop
            }

            m = PlayerEntityPattern.matchEntire(line)
            if (m != null) {
                val tag = PlayerTag()
                tag.EntityID = getEntityIdFromNameOrId(m.groupValues[1])
                tag.PlayerID = m.groupValues[2]

                if (mCurrentTag is CreateGameTag) {
                    (mCurrentTag as CreateGameTag).playerList.add(tag)
                }
                break@contentLoop
            }

            m = TAG.matchEntire(line)
            if (m != null) {
                val key = m.groupValues[1]
                val value = m.groupValues[2]

                if (mCurrentTag is CreateGameTag) {
                    if ((mCurrentTag as CreateGameTag).playerList.size > 0) {
                        (mCurrentTag as CreateGameTag).playerList[(mCurrentTag as CreateGameTag).playerList.size - 1].tags[key] = value
                    } else  {
                        (mCurrentTag as CreateGameTag).gameEntity.tags[key] = value
                    }
                } else if (mCurrentTag is ShowEntityTag) {
                    (mCurrentTag as ShowEntityTag).tags[key] = value
                } else if (mCurrentTag is FullEntityTag) {
                    (mCurrentTag as FullEntityTag).tags[key] = value
                } else {
                    log("got tag= outside of valid tag")
                }
                break@contentLoop
            }

            m = INFO.matchEntire(line)
            if (m != null) {
                if (mCurrentTag is MetaDataTag) {
                    (mCurrentTag as MetaDataTag).Info.add(getEntityIdFromNameOrId(m.groupValues[1])!!)
                }
                break@contentLoop
            }

            break@contentLoop
        }
    }

    private fun handleDebugPrintGame(line: String) {
        var m: MatchResult?

        m = BUILD_NUMBER.matchEntire(line)
        if (m != null) {
            mTagConsumer(BuildNumberTag(m.groupValues[1]))
            return
        }

        m = GAME_TYPE.matchEntire(line)
        if (m != null) {
            mTagConsumer(GameTypeTag(m.groupValues[1]))
            return
        }

        m = FORMAT_TYPE.matchEntire(line)
        if (m != null) {
            mTagConsumer(FormatTypeTag(m.groupValues[1]))
            return
        }

        m = SCENARIO_ID.matchEntire(line)
        if (m != null) {
            mTagConsumer(ScenarioIdTag(m.groupValues[1]))
            return
        }

        m = PLAYER_MAPPING.matchEntire(line)
        if (m != null) {
            mTagConsumer(PlayerMappingTag(m.groupValues[1], m.groupValues[2]))
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
