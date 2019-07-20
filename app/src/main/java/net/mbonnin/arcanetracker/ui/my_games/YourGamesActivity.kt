package net.mbonnin.arcanetracker.ui.my_games

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.history_activity.*
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.model.GameSummary

class YourGamesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.history_activity)

        val adapter = GameAdapter(GameSummary.gameSummaryList ?: emptyList())

        recyclerView.layoutManager = LinearLayoutManager(ArcaneTrackerApplication.context)
        recyclerView.adapter = adapter

        if (adapter.itemCount == 0) {
            historyEmpty.visibility = View.VISIBLE
            eraseHistory.visibility = View.GONE
        } else {
            historyEmpty.visibility = View.GONE
        }

        eraseHistory.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(R.string.history_delete_confirmation)
                    .setPositiveButton(R.string.delete_everything) { _, _ ->
                        GameSummary.eraseGameSummary()
                        adapter.notifyDataSetChanged()
                        historyEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()


        }

        gameCount.setText(this.getString(R.string.gameCount, adapter.itemCount))
    }
}
