package net.hearthsim.hslog

import kotlinx.io.streams.asInput
import net.hearthsim.console.Console
import net.hearthsim.console.DefaultConsole
import net.hearthsim.hsmodel.CardJson
import org.junit.Test
import java.io.File

class PowerParserTest {
    val input = File("../app/src/main/res/raw/cards.json").inputStream().asInput()
    val cardJson = CardJson(lang = "enUS", injectedCards = null, input = input)

    val console = object : Console {
        override fun debug(message: String) {
            val regex = Regex("[A-Z0-9]{3}_[0-9]{3}")
            val m = message.replace(regex) {
                val cardId = it.groupValues[0]
                "$cardId (${cardJson.getCard(cardId).name})"
            }
            println(m)
        }

        override fun error(message: String) {
            debug(message)
        }

        override fun error(throwable: Throwable) {
            throwable.printStackTrace()
        }

    }
    val hsLog = HSLog(console = console, cardJson = cardJson)

    @Test
    fun `magnetized test`() {
        val dir = System.getProperty("user.dir")
        val powerLines = File(dir, "src/jvmTest/files/power.log").readLines()

        hsLog.setListener(object : DefaultHSLogListener() {

        })
        powerLines.forEach {
            hsLog.processPower(it, false)
        }

        console.debug("victory=${hsLog.currentOrFinishedGame()!!.victory}")
    }
}