package net.hearthsim.hslog.parser.power


data class BattlegroundsMinion(
        val CardId: String,
        val attack: Int,
        val health: Int,
        val poisonous: Boolean,
        val divineShield: Boolean
)