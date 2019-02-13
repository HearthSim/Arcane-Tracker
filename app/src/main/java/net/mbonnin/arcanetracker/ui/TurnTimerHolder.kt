package net.mbonnin.arcanetracker.ui

import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.turn_timer.*
import net.mbonnin.arcanetracker.HDTApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager
import net.mbonnin.arcanetracker.parser.Entity
import net.mbonnin.arcanetracker.parser.Game
import net.mbonnin.arcanetracker.parser.GameLogic
import timber.log.Timber
import java.util.*

class TurnTimerHolder(override val containerView: View) : GameLogic.Listener, LayoutContainer {
    var displayed = false
    val viewManager = net.mbonnin.arcanetracker.ViewManager.get()
    var lastTurn = -1
    var game: Game? = null
    var turnStart = 0L
    var turnDuration = 0L
    var playerSum = 0L
    var opponentSum = 0L
    var isPlayer = false

    val handler = Handler()

    fun friendlyDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60

        return String.format(Locale.US, "%d:%02d", minutes, seconds % 60)
    }
    val updateRunnable: Runnable = object: Runnable {
        override fun run() {
            if (lastTurn < 0) {
                containerView.visibility = GONE
                handler.postDelayed(this, 1000)
                return
            }
            containerView.visibility = VISIBLE

            val now = System.currentTimeMillis()
            var millis = turnDuration - (now - turnStart)
            if (millis < 0) {
                millis = 0
            }

            turn.text = friendlyDuration(millis)

            var playerMillis = playerSum
            if (isPlayer) {
                playerMillis +=  now - turnStart
            }
            player.text = friendlyDuration(playerMillis)

            var opponentMillis = opponentSum
            if (!isPlayer) {
                opponentMillis +=  now - turnStart
            }
            opponent.text = friendlyDuration(opponentMillis)

            handler.postDelayed(this, 1000)
        }
    }

    override fun gameStarted(game: Game) {
        if (!displayed) {
            val params = ViewManager.Params()

            val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(Utils.dpToPx(70), View.MeasureSpec.EXACTLY)
            val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            containerView.measure(wMeasureSpec, hMeasureSpec)

            params.x = viewManager.width - containerView.measuredWidth - Utils.dpToPx(8)
            params.y = (viewManager.height - containerView.measuredHeight) / 2
            params.w = containerView.measuredWidth
            params.h = containerView.measuredHeight
            viewManager.addView(containerView, params)
            displayed = true
            handler.post(updateRunnable)

            playerSum = 0
            opponentSum = 0
        }

        this.game = game
    }

    override fun gameOver() {
        if (displayed) {
            viewManager.removeView(containerView)
            handler.removeCallbacks(updateRunnable)
        }
        lastTurn = -1
        game = null
    }

    override fun somethingChanged() {
        val turn = game?.gameEntity?.tags?.get(Entity.KEY_TURN)?.toIntOrNull()
        if (turn == null || turn == lastTurn) {
            return
        }
        val currentPlayer = game?.player?.entity?.tags?.get(Entity.KEY_CURRENT_PLAYER)?.toIntOrNull()
                ?: return
        if (game?.player?.entity?.tags?.get(Entity.KEY_MULLIGAN_STATE) != "DONE") {
            return
        }
        if (game?.opponent?.entity?.tags?.get(Entity.KEY_MULLIGAN_STATE) != "DONE") {
            return
        }

        isPlayer = currentPlayer == 1

        if (turn > 1) {
            if (isPlayer) {
                opponentSum += System.currentTimeMillis() - turnStart
            } else {
                playerSum += System.currentTimeMillis() - turnStart
            }
        }
        turnStart = System.currentTimeMillis()
        val player = if (currentPlayer == 1) game?.player else game?.opponent
        turnDuration = (player?.entity?.tags?.get(Entity.KEY_TIMEOUT)?.toLongOrNull() ?: 99) * 1000
        lastTurn = turn

        Timber.d("starting turn=$turn isPlayer=$currentPlayer timeout=${turnDuration}")
    }

    companion object {
        fun start() {
            val view = LayoutInflater.from(HDTApplication.context).inflate(R.layout.turn_timer, null, false)
            val holder = TurnTimerHolder(view)

            GameLogic.get().addListener(holder)
        }
    }
}