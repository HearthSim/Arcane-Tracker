package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class LogsDeckAdapter : RecyclerView.Adapter<LogsDeckAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_deck, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return LogsDeckList.get().size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deck = LogsDeckList.get()[position]

        holder.deckBackground.setImageDrawable(Utils.getDrawableForClassIndex(deck.classIndex))
        holder.deckName.setText(deck.name)
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val deckBackground = view.findViewById<ImageView>(R.id.deckBackground)
        val deckName = view.findViewById<TextView>(R.id.deckName)
    }
}

