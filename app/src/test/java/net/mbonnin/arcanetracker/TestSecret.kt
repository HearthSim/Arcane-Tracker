package net.mbonnin.arcanetracker

import kotlinx.io.streams.asInput
import net.mbonnin.hsmodel.CardJson
import org.junit.Assert
import org.junit.Test
import java.io.File

class TestSecret {
    @Test
    fun run() {
        val input = File("/home/martin/git/Arcane-Tracker/app/src/main/res/raw/cards.json").inputStream().asInput()
        CardJson.init("enUS", null,  input)
        val possibleSecrets = CardUtil.possibleSecretList("PALADIN", "GT_ARENA", "FT_WILD")

        Assert.assertTrue(!possibleSecrets.isEmpty())
    }
}