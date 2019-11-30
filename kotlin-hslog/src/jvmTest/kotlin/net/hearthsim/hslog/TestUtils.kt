package net.hearthsim.hslog

import kotlinx.io.streams.asInput
import net.hearthsim.console.Console
import net.hearthsim.hsmodel.CardJson
import java.io.File

object  TestUtils {
    val console = object : Console {
        override fun debug(message: String) {
            /**
             * A small hack to resolve the card ids on the fly
             */
            val regex = Regex("[a-zA-Z0-9]{2,3}_[0-9]{3}")
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

    val cardJson by lazy {
        val input = File("../app/src/main/res/raw/cards.json").inputStream().asInput()
        CardJson.fromMultiLangJson(lang = "enUS", injectedCards = emptyList(), input = input)
    }

    fun newHSLog() = HSLog(console = console, cardJson = cardJson, debounceDelay = 0)
}