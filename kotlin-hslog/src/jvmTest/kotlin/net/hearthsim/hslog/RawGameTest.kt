package net.hearthsim.hslog

import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.enum.CardId
import org.junit.Test
import java.io.File

class RawGameTest {
    @Test
    fun `battlegrounds file produces a raw game`() {
        val dir = System.getProperty("user.dir")
        val powerLines = File("/home/martin/dev/hsdata/2019_11_11_battlegrounds").readLines()
        val hsLog = TestUtils.newHSLog()

        var rawGame: ByteArray? = null

        hsLog.setListener(object : DefaultHSLogListener() {
            override fun onRawGame(gameString: ByteArray, gameStartMillis: Long) {
                rawGame = gameString
            }
        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        assert(rawGame != null)
    }
}