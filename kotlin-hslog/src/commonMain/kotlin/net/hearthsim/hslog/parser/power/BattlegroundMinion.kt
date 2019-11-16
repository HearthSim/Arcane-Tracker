package net.hearthsim.hslog.parser.power

import net.hearthsim.hsmodel.enum.CardId

data class BattlegroundMinion(
        val CardId: String,
        val attack: Int,
        val defense: Int,
        val poisonous: Boolean,
        val divineShield: Boolean
)