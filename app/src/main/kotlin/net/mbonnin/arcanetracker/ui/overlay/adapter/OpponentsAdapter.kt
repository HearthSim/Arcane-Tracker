package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.graphics.Path
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.hearthsim.hslog.util.allPlayerClasses
import net.hearthsim.hsmodel.enum.PlayerClass
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.helper.HeroUtil
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.sqldelight.mainDatabase
import timber.log.Timber

class OpponentsAdapter(val deckId: String) : RecyclerView.Adapter<OpponentsAdapter.ViewHolder>() {
    val allPlayerClasses = allPlayerClasses().filter { it != PlayerClass.NEUTRAL }

    class Opponent(val playerClass: String, val total: Int, val won: Int)

    var opponentList = emptyList<Opponent>()

    init {
        GlobalScope.launch(Dispatchers.Main) {
            opponentList = allPlayerClasses.map {
                val total = async {
                    mainDatabase.gameQueries.totalPlayedAgainst(deck_id = deckId, opponent_class = it).executeAsOne()
                }
                val won = async {
                    mainDatabase.gameQueries.totalVictoriesAgainst(deck_id = deckId, opponent_class = it).executeAsOne()
                }
                Opponent(playerClass = it, total = total.await().toInt(), won = won.await().toInt())
            }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_opponent, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        if (opponentList.isEmpty()) {
            return 0
        } else {
            return opponentList.size + 2
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.imageView.visibility = GONE
            holder.total.visibility = VISIBLE
            holder.total.setText(Utils.getString(R.string.opponent))

            holder.played.setText(Utils.getString(R.string.played))
            holder.won.setText(Utils.getString(R.string.won))
            holder.lost.setText(Utils.getString(R.string.lost))
            holder.winRate.setText(Utils.getString(R.string.winRate))
        } else {
            val total: Int
            val won: Int

            if (position == opponentList.size + 1) {
                holder.imageView.visibility = GONE
                holder.total.setText(Utils.getString(R.string.total))
                holder.total.visibility = VISIBLE

                total = opponentList.fold(0, { acc, opponent -> acc + opponent.total })
                won = opponentList.fold(0, { acc, opponent -> acc + opponent.won })
            } else {
                val opponent = opponentList[position - 1]
                total = opponent.total
                won = opponent.won

                holder.imageView.visibility = VISIBLE
                holder.imageView.setImageDrawable(HeroUtil.getDrawable(opponent.playerClass))
                holder.total.visibility = GONE
            }

            holder.played.setText(total.toString())
            holder.won.setText(won.toString())
            holder.lost.setText((total - won).toString())

            val winRateText = if (total > 0) {
                String.format("%d%%", (100 * won)/total)
            } else {
                Utils.getString(R.string.na)
            }
            holder.winRate.setText(winRateText)
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val total = view.findViewById<TextView>(R.id.total)
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val played = view.findViewById<TextView>(R.id.played)
        val won = view.findViewById<TextView>(R.id.won)
        val lost = view.findViewById<TextView>(R.id.lost)
        val winRate = view.findViewById<TextView>(R.id.winRate)
    }
}

