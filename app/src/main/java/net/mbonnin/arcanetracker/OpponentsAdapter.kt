package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.hsmodel.PlayerClass
import timber.log.Timber

class OpponentsAdapter(val deckId: String) : RecyclerView.Adapter<OpponentsAdapter.ViewHolder>() {
    val heroes = allHeroes().filter { it != PlayerClass.NEUTRAL }

    class Opponent(val playerClass: String, val total: Int, val won: Int)

    val opponentList = mutableListOf<Opponent>()

    init {

        val singleList = heroes.map {
            Single.zip(Single.just(it),
                    RDatabaseSingleton.instance.gameDao().totalPlayedAgainst(deckId, it).toSingle().onErrorReturn { 0 },
                    RDatabaseSingleton.instance.gameDao().totalVictoriesAgainst(deckId, it).toSingle().onErrorReturn { 0 },
                    Function3<String, Int, Int, Opponent> { playerClass, total, won ->
                        Opponent(playerClass, total, won)
                    })
        }
        Single.concat(singleList)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    opponentList.clear()
                    opponentList.addAll(it)
                    notifyDataSetChanged()
                }, {
                    Timber.e(it)
                })
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

