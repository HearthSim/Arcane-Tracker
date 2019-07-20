package net.mbonnin.arcanetracker.ui.my_games

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.model.GameSummary

import androidx.recyclerview.widget.RecyclerView

class GameAdapter(private val mGameList: List<GameSummary>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.game_view, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as GameViewHolder).bind(mGameList[position], position)
    }

    override fun getItemCount(): Int {
        return mGameList.size
    }

}
