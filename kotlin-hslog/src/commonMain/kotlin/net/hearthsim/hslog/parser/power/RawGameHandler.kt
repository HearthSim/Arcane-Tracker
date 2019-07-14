package net.hearthsim.hslog.parser.power

import com.soywiz.klock.DateTime
import kotlin.math.log

class RawGameHandler(private val rawGameConsumer: ((rawGame: String, unixMillis: Long) -> Unit)?) {
    private val rawBuilder = StringBuilder()
    private var rawMatchStart: Long = 0
    private var rawGoldRewardStateCount: Int = 0

    /**
     * This will be fed both GameState and PowerTaskList logs so that they can be sent to HSReplay
     */
    fun process(rawLine: String) {

        if (rawLine.contains("CREATE_GAME") && rawLine.contains("GameState")) {
            // This assumes GameState logs always happen before the PowerTaskList ones
            rawBuilder.clear()
            rawMatchStart = DateTime.now().unixMillisLong

            rawGoldRewardStateCount = 0
        }

        rawBuilder.append(rawLine)
        rawBuilder.append('\n')

        if (rawLine.contains("GOLD_REWARD_STATE")) {
            rawGoldRewardStateCount++

            // 4 = 2 (for GameState) + 2 (for Power TaskList)
            // We want to make sure the GameLogic has the game info when we send the logs to HSReplay
            if (rawGoldRewardStateCount == 4) {
                val gameStr = rawBuilder.toString()

                rawGameConsumer?.invoke(gameStr, rawMatchStart)
            }
        }
    }


}