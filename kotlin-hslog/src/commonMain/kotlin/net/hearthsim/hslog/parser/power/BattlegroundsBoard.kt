package net.hearthsim.hslog.parser.power

data class BattlegroundsBoard(
        val currentTurn: Int = 0,
        val heroCardId: String,
        // from 0 (leader) to 7
        val leaderBoardPosition: Int,
        val turn: Int,
        val minions: List<BattlegroundsMinion>
)