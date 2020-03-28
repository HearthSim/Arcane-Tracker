package net.hearthsim.hslog.parser.power

import com.soywiz.klock.DateTime
import io.ktor.utils.io.core.toByteArray

class RawGameHandler(private val rawGameConsumer: ((rawGame: ByteArray, unixMillis: Long) -> Unit)?) {
    private val rawBuilder = MyByteArrayOutputStream()
    private var rawMatchStart: Long = 0
    private var rawGoldRewardStateCount: Int = 0
    private var isBattleGrounds = false

    /**
     * This will be fed both GameState and PowerTaskList logs so that they can be sent to HSReplay
     */
    fun process(rawLine: String) {

        if (rawLine.contains("CREATE_GAME") && rawLine.contains("GameState")) {
            // This assumes GameState logs always happen before the PowerTaskList ones
            rawBuilder.clear()
            rawMatchStart = DateTime.now().unixMillisLong

            rawGoldRewardStateCount = 0
            isBattleGrounds = false
        }

        rawBuilder.write(rawLine.toByteArray())
        rawBuilder.write("\n".toByteArray())

        if (rawLine.contains("GameType=GT_BATTLEGROUNDS")) {
            isBattleGrounds = true
        }

        if (rawLine.contains("GOLD_REWARD_STATE")) {
            rawGoldRewardStateCount++

            val max = if (!isBattleGrounds) {
                // 4 = 2 (for GameState) + 2 (for Power TaskList)
                // We want to make sure the GameLogic has the game info when we send the logs to HSReplay
                4
            } else {
                // BattleGrounds has no opponent so there will be only 2 GOLD_REWARD_STATE
                2
            }
            if (rawGoldRewardStateCount == max) {
                val gameStr = rawBuilder.bytes()

                rawGameConsumer?.invoke(gameStr, rawMatchStart)
            }
        }
    }


}