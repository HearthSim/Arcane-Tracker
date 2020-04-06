object GameUtil {
    fun gameTurnToHumanTurn(turn: Int): Int {
        return (turn + 1) / 2
    }
}