package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.Utils.runOnMainThread
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import timber.log.Timber

/**
 * the setters in this class are called from the ScreenCapture thread while the getters
 * are called from the mainThread, hence the volatile
 */
object RankHolder {
    @Volatile
    var playerRank = RANK_UNKNOWN
    @Volatile
    var opponentRank = RANK_UNKNOWN

    private fun displayToast(toast: String) {
        runOnMainThread({
            Toaster.show(toast)
        })
    }

    @Synchronized
    fun registerRanks(playerRank: Int, opponentRank: Int) {
        val sb = StringBuilder()
        if (playerRank != RANK_UNKNOWN) {
            if (this.playerRank != playerRank) {
                this.playerRank = playerRank
                sb.append(HDTApplication.context.getString(R.string.your_rank, playerRank))
            }
        }

        if (opponentRank != RANK_UNKNOWN) {
            if (this.opponentRank != opponentRank) {
                this.opponentRank = opponentRank

                if (!sb.isBlank()) {
                    sb.append(" - ")
                }
                sb.append(HDTApplication.context.getString(R.string.opponent_rank, opponentRank))
            }
        }

        if (!sb.isBlank()) {
            displayToast(sb.toString())
            Timber.d("playerRank=$playerRank opponentRank=$opponentRank")
        }
    }

    @Synchronized
    fun reset() {
        playerRank = RANK_UNKNOWN
        opponentRank = RANK_UNKNOWN
    }
}
