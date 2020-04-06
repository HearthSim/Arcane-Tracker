package net.mbonnin.arcanetracker.ui.my_games

import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.game_view.*
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.getPlayerClassDisplayName
import net.mbonnin.arcanetracker.helper.getPlayerClassRoundIcon
import net.mbonnin.arcanetracker.sqldelight.Game

class GameViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    lateinit var summary: Game

    init {
        hsReplay.setOnClickListener {
            if (summary.hs_replay_url == null) {
                Toast.makeText(containerView.context, containerView.context.getString(R.string.could_not_find_replay), Toast.LENGTH_LONG).show()
                return@setOnClickListener

            }
            summary.hs_replay_url?.let {
                Utils.openLink(it)
            }
        }
    }

    fun bind(summary: Game, position: Int) {
        this.summary = summary

        if (position % 2 == 1) {
            containerView.setBackgroundColor(Color.BLACK)
        } else {
            containerView.background = null
        }
        val context = itemView.context
        hero.setImageResource(getPlayerClassRoundIcon(summary.player_player_class))
        opponentHero.setImageResource(getPlayerClassRoundIcon(summary.opponent_player_class))
        deckName.text = summary.deck_name
        winLoss.text = if (summary.victory ?: 0 > 0) context.getString(R.string.win) else context.getString(R.string.lost)
        coin.visibility = if (summary.coin > 0) View.VISIBLE else View.INVISIBLE
        opponentName.text = getPlayerClassDisplayName(summary.opponent_player_class)

        hsReplay.visibility = if (!summary.hs_replay_url.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
    }
}
