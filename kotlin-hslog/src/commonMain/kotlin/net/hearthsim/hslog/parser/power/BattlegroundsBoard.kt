package net.hearthsim.hslog.parser.power

/*
 * It's pretty important that this class is a data class to debounce emissions
 * to the listeners
 */
data class BattlegroundsBoard(
        val currentTurn: Int = 0,
        val leaderboardPlace: Int = 0,
        val heroCardId: String,
        val turn: Int,
        val minions: List<BattlegroundsMinion>
)