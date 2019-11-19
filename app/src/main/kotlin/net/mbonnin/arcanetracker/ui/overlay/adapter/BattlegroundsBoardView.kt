package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.battlegrounds_minion.view.*
import net.mbonnin.arcanetracker.R
import net.hearthsim.hslog.parser.power.BattlegroundsBoard
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.Utils

class BattlegroundsBoardView(context: Context) : ConstraintLayout(context) {
    init {
        LayoutInflater.from(context).inflate(R.layout.battlegrounds_board, this, true)
    }

    fun configure(board: BattlegroundsBoard) {
        findViewById<TextView>(R.id.age).text = context.getString(R.string.battlegrounds_turn, (board.currentTurn - board.turn) / 2)
        board.minions
                .take(7)
                .forEachIndexed { index, minion ->
                    val frameLayout = getChildAt(index) as FrameLayout

                    val minionView = LayoutInflater.from(context).inflate(R.layout.battlegrounds_minion, frameLayout, true)

                    val card = ArcaneTrackerApplication.get().cardJson.getCard(minion.CardId)

                    Picasso.with(context)
                            .load(Utils.getTileUrl(card.id))
                            .placeholder(R.drawable.hero_10)
                            .into(minionView.findViewById<TextView>(R.id.background) as ImageView)

                    minionView.findViewById<TextView>(R.id.attack).text = minion.attack.toString()
                    minionView.findViewById<TextView>(R.id.health).text = minion.health.toString()
                    minionView.findViewById<TextView>(R.id.name).text = card.name
                    minionView.findViewById<View>(R.id.divine_shield_image).alpha = if (minion.divineShield) 1f else 0f
                    minionView.findViewById<View>(R.id.poisonous_image).alpha = if (minion.poisonous) 1f else 0f
                }
    }
}