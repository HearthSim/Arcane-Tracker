package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import timber.log.Timber
import java.util.*

@SuppressLint("StaticFieldLeak")
object TurnTimer {

    val view = TurnTimerView(ArcaneTrackerApplication.context)
    val viewManager = ViewManager.get()
    var job: Job? = null

    var playerSum = 0L
    var opponentSum = 0L

    var game: Game? = null
    var isPlayer = false
    var turnStartMillis = 0L

    private fun friendlyDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60

        return String.format(Locale.US, "%d:%02d", minutes, seconds % 60)
    }

    fun gameEnd(game: Game) {
        playerSum = 0L
        opponentSum = 0L
        turnStartMillis = 0L

        if (viewManager.contains(view)) {
            viewManager.removeView(view)
        }
        job?.cancel()
        job = null
    }

    fun displayView() {
        val params = ViewManager.Params()

        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(wMeasureSpec, hMeasureSpec)

        /**
         * This was an attempt to position the Turn Counter to the left of the "turn" button but it's too much hassle making it work
         * for all configurations (tablets, phones, notch, etc...). Sometimes black bars are added too which makes thing even more
         * complicated.
         * Maybe the easiest solution is to run pattern matching on the turn button...
         *
         * Until then, I just position the View on the right of the button
         */
        /*var boardWidth = viewManager.height * 1.568f
        params.x = (viewManager.width - (viewManager.width - boardWidth) / 2 - 0.16 * boardWidth - Utils.dpToPx(8) - view.measuredWidth).toInt()*/
        params.x = viewManager.width - view.measuredWidth
        params.y = (((viewManager.height - view.measuredHeight) / 2) * 0.98).toInt()
        params.w = view.measuredWidth
        params.h = view.measuredHeight
        viewManager.addView(view, params)
    }

    fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {
        val now = System.currentTimeMillis()
        if (isPlayer && turnStartMillis > 0) {
            opponentSum += now - turnStartMillis
        } else if (!isPlayer && turnStartMillis > 0) {
            playerSum += now - turnStartMillis
        }
        turnStartMillis = now
        this.isPlayer = isPlayer
        this.game = game

        if (job == null) {
            job = GlobalScope.launch(Dispatchers.Main) {
                update()
            }
        }
    }

    suspend fun update() {
        while (true) {
            val ellapsed = (System.currentTimeMillis() - turnStartMillis)

            val player = if (isPlayer) game?.player else game?.opponent
            val timeout = player?.entity?.tags?.get(Entity.KEY_TIMEOUT)?.toIntOrNull()

            var turnTime = ""
            if (timeout != null) {
                var remaining = timeout * 1000 - ellapsed
                if (remaining < 0) {
                    remaining = 0
                }
                turnTime = friendlyDuration(remaining)
            }

            val totalEllapsedPlayer = if (isPlayer) {
                playerSum + ellapsed
            } else {
                playerSum
            }
            val playerTime = friendlyDuration(totalEllapsedPlayer)

            val totalEllapsedOpponent = if (!isPlayer) {
                opponentSum + ellapsed
            } else {
                opponentSum
            }
            val opponentTime = friendlyDuration(totalEllapsedOpponent)

            view.setValues(opponentTime, turnTime, playerTime)

            //Timber.d("opponent=$opponentTime turn=$turnTime player=$playerTime")

            if (Settings.get(Settings.TURN_TIMER_ENABLED, true) != true) {
                if (viewManager.contains(view)) {
                    viewManager.removeView(view)
                }
            } else {
                if (!viewManager.contains(view)) {
                    displayView()
                }
            }

            delay(1000)
        }

    }
}