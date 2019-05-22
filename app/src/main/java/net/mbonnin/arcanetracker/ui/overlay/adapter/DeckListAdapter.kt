package net.mbonnin.arcanetracker.ui.overlay.adapter

import androidx.recyclerview.widget.RecyclerView
import net.mbonnin.arcanetracker.hslog.Deck

abstract class DeckListAdapter:  RecyclerView.Adapter< RecyclerView.ViewHolder>() {
    abstract fun setOnDeckSelectedListener(listener: (deck: Deck) -> Unit)
}