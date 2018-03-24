package net.mbonnin.arcanetracker

import net.mbonnin.arcanetracker.Utils.runOnMainThread
import net.mbonnin.arcanetracker.detector.RANK_UNKNOWN
import timber.log.Timber
import java.util.*

/**
 * the setters in this class are called from the ScreenCapture thread while the getters
 * are called from the mainThread, hence the volatile
 */
object FMRHolder {
    @Volatile var playerRank = RANK_UNKNOWN
    @Volatile var opponentRank = RANK_UNKNOWN

    var rank = RANK_UNKNOWN
        set(value) {
            if (value != field) {
                field = value
            }
        }


    private fun displayToast(toast: String) {
        runOnMainThread({
            Toaster.show(toast)
        })
    }

    fun registerRanks(playerRank: Int, opponentRank: Int) {
        if (this.playerRank != playerRank || this.opponentRank != opponentRank) {
            this.playerRank = playerRank
            this.opponentRank = opponentRank

            val sb = StringBuilder()
            if (playerRank != RANK_UNKNOWN) {
                sb.append(ArcaneTrackerApplication.context.getString(R.string.your_rank, playerRank))
            }
            if (opponentRank != RANK_UNKNOWN) {
                if (!sb.isBlank()) {
                    sb.append(" - ")
                }
                sb.append(ArcaneTrackerApplication.context.getString(R.string.opponent_rank, opponentRank))
            }

            displayToast(sb.toString())
            Timber.d("playerRank=$playerRank opponentRank=$opponentRank")
        }
    }
}
