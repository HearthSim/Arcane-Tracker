package net.mbonnin.arcanetracker

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*
import net.hearthsim.hslog.parser.power.Game
import java.util.*

@SuppressLint("StaticFieldLeak")
object TurnTimer {

    val view = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.turn_timer, null, false)
    val viewManager = ViewManager.get()
    var job: Job? = null

    var playerSum = 0L
    var opponentSum = 0L
    var playerStartMillis = 0L
    var opponentStartMillis = 0L

    var timeout: Long? = null
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

        if (viewManager.contains(view)) {
            viewManager.removeView(view)
        }
        job?.cancel()
        job = null
    }

    fun onTurn(game: Game, turn: Int, isPlayer: Boolean, timeout: Int?) {
        if (!viewManager.contains(view)) {
            val params = ViewManager.Params()

            val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(Utils.dpToPx(70), View.MeasureSpec.EXACTLY)
            val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            view.measure(wMeasureSpec, hMeasureSpec)

            params.x = viewManager.width - view.measuredWidth - Utils.dpToPx(8)
            params.y = (viewManager.height - view.measuredHeight) / 2
            params.w = view.measuredWidth
            params.h = view.measuredHeight
            viewManager.addView(view, params)
        }

        val now = System.currentTimeMillis()
        if (isPlayer && turnStartMillis > 0) {
            opponentSum += now - turnStartMillis
        } else if (!isPlayer && turnStartMillis > 0) {
            playerSum += now - turnStartMillis
        }
        turnStartMillis = now
        this.timeout = timeout?.toLong()
        this.isPlayer = isPlayer

        if (job == null) {
            GlobalScope.launch(Dispatchers.Main) {
                update()
            }
        }
    }

    suspend fun update() {
        while (true) {
            val ellapsed = (System.currentTimeMillis() - turnStartMillis)
            if (timeout != null) {
                val remaining = timeout!! * 1000 - ellapsed
                view.findViewById<TextView>(R.id.turn).text = friendlyDuration(remaining)
            }

            val totalEllapsedPlayer = if (isPlayer) {
                playerSum + ellapsed
            } else {
                playerSum
            }
            view.findViewById<TextView>(R.id.player).text = friendlyDuration(totalEllapsedPlayer)

            val totalEllapsedOpponent = if (!isPlayer) {
                opponentSum + ellapsed
            } else {
                opponentSum
            }
            view.findViewById<TextView>(R.id.opponent).text = friendlyDuration(totalEllapsedOpponent)


            delay(1000)
        }

    }
}