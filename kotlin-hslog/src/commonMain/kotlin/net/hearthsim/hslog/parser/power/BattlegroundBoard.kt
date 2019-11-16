package net.hearthsim.hslog.parser.power

data class BattlegroundBoard(
        val opponentHero: Entity,
        val turn: Int,
        val minions: List<BattlegroundMinion>
)