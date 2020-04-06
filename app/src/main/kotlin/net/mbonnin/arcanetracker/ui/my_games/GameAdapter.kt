package net.mbonnin.arcanetracker.ui.my_games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.sqldelight.Game
import net.mbonnin.arcanetracker.sqldelight.mainDatabase

class GameAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var mGameList = emptyList<Game>()

    fun updateGames(gameList: List<Game>) {
        mGameList = gameList
        notifyDataSetChanged()
    }
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
