package net.mbonnin.arcanetracker.ui.my_games

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.sqldelight.runtime.coroutines.asFlow
import kotlinx.android.synthetic.main.history_activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.sqldelight.Game
import net.mbonnin.arcanetracker.sqldelight.mainDatabase

class YourGamesActivity : Activity() {

    private lateinit var job: Job
    private val adapter = GameAdapter()

    private fun updateUI(gameList: List<Game>) {
        adapter.updateGames(gameList)

        if (adapter.itemCount == 0) {
            historyEmpty.visibility = View.VISIBLE
            eraseHistory.visibility = View.GONE
        } else {
            historyEmpty.visibility = View.GONE
            eraseHistory.visibility = View.VISIBLE
        }

        eraseHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.history_delete_confirmation)
                .setPositiveButton(R.string.delete_everything) { _, _ ->
                    mainDatabase.gameQueries.deleteAllGames()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()


        }

        gameCount.setText(this.getString(R.string.gameCount, adapter.itemCount))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.history_activity)
        
        recyclerView.layoutManager = LinearLayoutManager(ArcaneTrackerApplication.context)
        recyclerView.adapter = adapter

        job = GlobalScope.launch(Dispatchers.Main) {
            mainDatabase.gameQueries.selectArenaAndPvPGames()
                .asFlow()
                .collect {
                    updateUI(it.executeAsList())
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
