package net.hearthsim.hslog

import kotlinx.coroutines.runBlocking
import kotlinx.io.streams.asInput
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsmodel.CardJson
import org.junit.Test
import java.io.File

class PowerParserTest {
    @Test
    fun testParser() {
        val dir = System.getProperty("user.dir")
        val powerLines = File(dir, "src/jvmTest/files/power.log").readLines()

        val console = DefaultConsole()
        val input = File("../app/src/main/res/raw/cards.json").inputStream().asInput()
        val cardJson = CardJson(lang = "enUS", injectedCards = null, input = input)

        val hsLog = HSLog(console = console, cardJson = cardJson)

        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        console.debug("victory=${hsLog.currentOrFinishedGame()!!.victory}")
    }
}