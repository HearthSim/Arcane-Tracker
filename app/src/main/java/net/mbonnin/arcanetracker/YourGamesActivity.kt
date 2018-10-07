package net.mbonnin.arcanetracker

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import kotlinx.android.synthetic.main.history_activity.*
import net.mbonnin.arcanetracker.model.GameSummary

class YourGamesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.history_activity)

        val adapter = GameAdapter(GameSummary.getGameSummary())

        recyclerView.layoutManager = LinearLayoutManager(HDTApplication.context)
        recyclerView.adapter = adapter

        if (adapter.itemCount == 0) {
            historyEmpty.visibility = View.VISIBLE
            eraseHistory.visibility = View.GONE
        } else {
            historyEmpty.visibility = View.GONE
        }

        eraseHistory.setOnClickListener { v ->
            AlertDialog.Builder(this)
                    .setMessage(R.string.history_delete_confirmation)
                    .setPositiveButton(R.string.delete_everything, { _, _ ->
                        GameSummary.eraseGameSummary()
                        adapter.notifyDataSetChanged()
                        historyEmpty.visibility = if (adapter.itemCount == 0) View.VISIBLE else View.GONE
                    })
                    .setNegativeButton(R.string.cancel, { dialog, _ ->
                        dialog.dismiss()
                    })
                    .show()


        }

        gameCount.setText(this.getString(R.string.gameCount, adapter.itemCount))
    }
}
