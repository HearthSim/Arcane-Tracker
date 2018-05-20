package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by martin on 10/19/16.
 */
class LegacyDeckListAdapter : DeckListAdapter() {
    lateinit internal var onDeckSelectedListener: (deck: Deck) -> Unit


    override fun setOnDeckSelectedListener(listener: (deck: Deck) -> Unit) {
        onDeckSelectedListener = listener

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.deck_line_view, null)
        return object : RecyclerView.ViewHolder(view) {

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView

        val deck: Deck
        if (position >= LegacyDeckList.get().size) {
            deck = LegacyDeckList.arenaDeck
        } else {
            deck = LegacyDeckList.get()[position]
        }

        view.setOnClickListener { v -> onDeckSelectedListener(deck) }

        (view.findViewById<View>(R.id.deckImageRound) as ImageView).setImageDrawable(Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", deck.classIndex + 1)))
        (view.findViewById<View>(R.id.deckName) as TextView).text = deck.name
    }

    override fun getItemCount(): Int {
        return LegacyDeckList.get().size + 1
    }
}
