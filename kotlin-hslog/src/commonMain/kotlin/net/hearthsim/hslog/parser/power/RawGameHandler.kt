package net.hearthsim.hslog.parser.power

import com.soywiz.klock.DateTime

internal class RawGameHandler(private val rawGameConsumer: ((rawGame: ByteArray, unixMillis: Long) -> Unit)?) {
    private val rawBuilder = MyByteArrayOutputStream()
    private var rawMatchStart: Long = 0
    private var isBattleGrounds = false
    private var stateCompleteCount = 0

    /**
     * This will be fed both GameState and PowerTaskList logs so that they can be sent to HSReplay
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun process(rawLine: String) {

        if (rawLine.contains("CREATE_GAME") && rawLine.contains("GameState")) {
            // This assumes GameState logs always happen before the PowerTaskList ones
            rawBuilder.clear()
            rawMatchStart = DateTime.now().unixMillisLong

            isBattleGrounds = false
            stateCompleteCount = 0
        }

        rawBuilder.write(rawLine.encodeToByteArray())
        rawBuilder.write("\n".encodeToByteArray())

        if (rawLine.contains("GameType=GT_BATTLEGROUNDS")) {
            isBattleGrounds = true
        }

        if (rawLine.contains("tag=STATE value=COMPLETE")) {
            // one for PowerState, one for GameState
            stateCompleteCount++
            if (stateCompleteCount == 2) {
                val gameStr = rawBuilder.bytes()

                rawGameConsumer?.invoke(gameStr, rawMatchStart)
            }
        }
    }
}