package net.mbonnin.arcanetracker

import androidx.recyclerview.widget.RecyclerView

abstract class DeckListAdapter:  RecyclerView.Adapter< RecyclerView.ViewHolder>() {
    abstract fun setOnDeckSelectedListener(listener: (deck: Deck) -> Unit)
}