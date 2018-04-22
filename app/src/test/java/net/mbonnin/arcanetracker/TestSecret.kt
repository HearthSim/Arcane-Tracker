package net.mbonnin.arcanetracker

import net.mbonnin.hsmodel.CardJson
import org.junit.Assert
import org.junit.Test

class TestSecret {
    @Test
    fun run() {
        CardJson.init("enUS")
        val possibleSecrets = CardUtil.possibleSecretList("PALADIN", "GT_ARENA", "FT_WILD")

        Assert.assertTrue(!possibleSecrets.isEmpty())
    }
}