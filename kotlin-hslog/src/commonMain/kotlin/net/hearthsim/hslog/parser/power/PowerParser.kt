package net.hearthsim.hslog.parser.power

import com.soywiz.klock.DateTime
import net.hearthsim.hslog.parser.LogLine
import net.hearthsim.hslog.parser.parseLineWithMethod
import net.hearthsim.hslog.parser.power.BlockTag.Companion.TYPE_ATTACK
import net.hearthsim.hslog.parser.power.BlockTag.Companion.TYPE_PLAY
import net.hearthsim.hslog.parser.power.BlockTag.Companion.TYPE_TRIGGER

/**
 * Created by martin on 10/27/16.
 */

class PowerParser(
        private val tagConsumer: (Tag) -> Unit,
        private val rawGameConsumer: ((rawGame: ByteArray, unixMillis: Long) -> Unit)?,
        private val logger: ((String, Array<out String>) -> Unit)?
) {

    companion object {
        private val BLOCK_START_PATTERN = Regex("BLOCK_START BlockType=(.*) Entity=(.*) EffectCardId=(.*) EffectIndex=(.*) Target=(.*) SubOption=(.*)")
        private val BLOCK_START_CONTINUATION_PATTERN = Regex("(.*) TriggerKeyword=(.*)")
        private val BLOCK_END_PATTERN = Regex("BLOCK_END")

        private val GameEntityPattern = Regex("GameEntity EntityID=(.*)")
        private val PlayerEntityPattern = Regex("Player EntityID=(.*) PlayerID=(.*) GameAccountId=(.*)")

        private val FULL_ENTITY = Regex("FULL_ENTITY - Updating (.*) CardID=(.*)")
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
    }

    private val rawGameHandler = RawGameHandler(rawGameConsumer)
    private val mBlockTagStack = ArrayList<BlockTag>()
    private var mCurrentTag: Tag? = null

    private fun log(format: String, vararg args: String) {
        logger?.invoke(format, args)
    }

    fun process(rawLine: String, isOldData: Boolean) {
        if (isOldData) {
            return
        }

        rawGameHandler.process(rawLine)

        if (rawLine.startsWith("================== Begin Spectating")) {
            tagConsumer(SpectatorTag(true))
            return
        } else if (rawLine.startsWith("================== End Spectator Mode")) {
            tagConsumer(SpectatorTag(false))
            return
        }

        val logLine = parseLineWithMethod(rawLine, logger) ?: return

        //log(logLine.line)
        val line = logLine.line.trim()

        if (logLine.method.startsWith("GameState.DebugPrintGame()")) {
            handleDebugPrintGame(line)
        } else if (logLine.method.startsWith("PowerTaskList.DebugPrintPower()")) {
            handleDebugPrintPower(line)
        }
    }

    private fun resynchronizeBlockStackIfNeeded() {
        if (mBlockTagStack.size > 0) {
            log("Resynchronize blocks")
            if (mCurrentTag != null) {
                mBlockTagStack[mBlockTagStack.size - 1].children.add(mCurrentTag!!)
            }
            tagConsumer(mBlockTagStack[0])
            mBlockTagStack.clear()
            mCurrentTag = null
        }
    }

    private fun handleDebugPrintPower(line: String) {
        var m: MatchResult?
        var newTag: Tag? = null

        if ("TAG_CHANGE Entity=GameEntity tag=STEP value=FINAL_GAMEOVER" == line) {
            /*
             *  it could happen that the game is stopped in the middle of a block
             */
            resynchronizeBlockStackIfNeeded()
        }

        tagLoop@ while (true) {
            if ("CREATE_GAME" == line) {
                /*
                 * reset any previous state in case there are 2 CREATE_GAME in a row
                 */
                mCurrentTag = null
                mBlockTagStack.clear()

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

            // Some battlegrounds files do not balance the BLOCK_START and BLOCK_END
            // This seems to be mainly about ATTACK block
            // see https://github.com/HearthSim/python-hslog/commit/63e9e41976cbec7ef95ced0f49f4b9a06c02cf3c
            if (tag.BlockType == TYPE_PLAY) {
                // PLAY is always at the root
                resynchronizeBlockStackIfNeeded()
            } else if (mBlockTagStack.size > 0
                    && mBlockTagStack[mBlockTagStack.size - 1].BlockType == TYPE_ATTACK
                    && tag.BlockType != TYPE_TRIGGER
            ) {
                // Attack blocks should only have TRIGGER beneath them. If something else, it certainly
                // means the ATTACK block wasn't correctly closed
            }

            if (mBlockTagStack.size > 0) {
                mBlockTagStack[mBlockTagStack.size - 1].children.add(tag)
            }
            mBlockTagStack.add(tag)
            return
        }

        if (BLOCK_END_PATTERN.matchEntire(line) != null) {
            openNewTag(null)
            if (mBlockTagStack.size > 0) {
                val blockTag = mBlockTagStack.removeAt(mBlockTagStack.size - 1)
                if (mBlockTagStack.size == 0) {
                    tagConsumer(blockTag)
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
            tagConsumer(BuildNumberTag(m.groupValues[1]))
            return
        }

        m = GAME_TYPE.matchEntire(line)
        if (m != null) {
            tagConsumer(GameTypeTag(m.groupValues[1]))
            return
        }

        m = FORMAT_TYPE.matchEntire(line)
        if (m != null) {
            tagConsumer(FormatTypeTag(m.groupValues[1]))
            return
        }

        m = SCENARIO_ID.matchEntire(line)
        if (m != null) {
            tagConsumer(ScenarioIdTag(m.groupValues[1]))
            return
        }

        m = PLAYER_MAPPING.matchEntire(line)
        if (m != null) {
            tagConsumer(PlayerMappingTag(m.groupValues[1], m.groupValues[2]))
            return
        }
    }


    private fun openNewTag(newTag: Tag?) {
        if (mCurrentTag != null) {
            if (mBlockTagStack.size > 0) {
                mBlockTagStack[mBlockTagStack.size - 1].children.add(mCurrentTag!!)
            } else {
                tagConsumer(mCurrentTag!!)
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
