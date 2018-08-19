package net.mbonnin.arcanetracker

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.Toast
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.game_view.*
import net.mbonnin.arcanetracker.helper.getDisplayName
import net.mbonnin.arcanetracker.model.GameSummary

class GameViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    lateinit var summary: GameSummary

    init {
        hsReplay.setOnClickListener {
            if (summary.hsreplayUrl == null) {
                Toast.makeText(containerView.context, containerView.context.getString(R.string.could_not_find_replay), Toast.LENGTH_LONG).show()
                return@setOnClickListener

            }
            Utils.openLink(summary.hsreplayUrl)
        }
    }

    fun bind(summary: GameSummary, position: Int) {
        this.summary = summary

        if (position % 2 == 1) {
            containerView.setBackgroundColor(Color.BLACK)
        } else {
            containerView.background = null
        }
        val context = itemView.context
        hero.setImageDrawable(Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", summary.hero + 1)))
        opponentHero.setImageDrawable(Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", summary.opponentHero + 1)))
        deckName.text = summary.deckName
        winLoss.text = if (summary.win) context.getString(R.string.win) else context.getString(R.string.loss)
        coin.visibility = if (summary.coin) View.VISIBLE else View.INVISIBLE
        opponentName.text = getDisplayName(summary.opponentHero)

        hsReplay.visibility = if (!summary.hsreplayUrl.isNullOrBlank()) View.VISIBLE else View.INVISIBLE
    }
}
