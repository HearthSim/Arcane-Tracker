package net.hearthsim.hslog.parser.power

data class BattlegroundsBoard(
        val currentTurn: Int = 0,
        val opponentHero: Entity,
        val turn: Int,
        val minions: List<BattlegroundsMinion>
)