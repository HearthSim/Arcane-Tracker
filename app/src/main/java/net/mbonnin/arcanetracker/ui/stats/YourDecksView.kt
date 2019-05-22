package net.mbonnin.arcanetracker.ui.stats

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.DisplayMetrics
import net.mbonnin.arcanetracker.hslog.Deck
import net.mbonnin.arcanetracker.SpacesItemDecoration


class YourDecksView(context: Context, val onDeckClicked: (Deck) -> Unit): RecyclerView(context) {
    val yourDecksAdapter = YourDecksAdapter()

    init {
        layoutManager = GridLayoutManager(context, 3)
        yourDecksAdapter.setOnClickListener{deckClickedId ->
            val deck = yourDecksAdapter.list.filter { it.id == deckClickedId }.firstOrNull()
            if (deck != null) {
                onDeckClicked(deck)
            }
        }
        adapter = yourDecksAdapter

        val spacing = 8.toPixel(context.resources.displayMetrics)

        setPadding(spacing, spacing, spacing, spacing)
        clipToPadding = false

        addItemDecoration(SpacesItemDecoration(spacing))
    }
}

fun Int.toPixel(displayMetrics: DisplayMetrics): Int {
    return (this.toFloat() * displayMetrics.density).toInt()
}

fun Int.toPixelFloat(displayMetrics: DisplayMetrics): Float {
    return (this.toFloat() * displayMetrics.density)
}