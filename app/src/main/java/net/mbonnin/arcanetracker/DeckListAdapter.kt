package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView

abstract class DeckListAdapter:  RecyclerView.Adapter< RecyclerView.ViewHolder>() {
    abstract fun setOnDeckSelectedListener(listener: (deck: Deck) -> Unit)
}