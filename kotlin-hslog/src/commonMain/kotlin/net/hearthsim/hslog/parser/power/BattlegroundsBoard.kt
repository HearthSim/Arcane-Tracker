package net.hearthsim.hslog.parser.power

data class BattlegroundsBoard(
        val currentTurn: Int = 0,
        val heroCardId: String,
        // As in the Game: from 1 to 8
        val leaderBoardPlace: Int,
        val turn: Int,
        val minions: List<BattlegroundsMinion>
)