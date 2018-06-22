package net.mbonnin.arcanetracker

import net.mbonnin.hsmodel.CardJson
import org.junit.Test

class TestLanguages {
    @Test
    fun run() {
        Constant.LANGUAGE_LIST.forEach {
            CardJson.init(it)
        }
    }
}