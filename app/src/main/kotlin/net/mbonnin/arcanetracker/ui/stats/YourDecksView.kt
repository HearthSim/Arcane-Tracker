package net.mbonnin.arcanetracker.ui.stats

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.DisplayMetrics
import android.widget.FrameLayout
import net.hearthsim.hslog.parser.decks.Deck
import net.mbonnin.arcanetracker.SpacesItemDecoration
import net.mbonnin.arcanetracker.ui.promo.PromoView


class YourDecksView(context: Context, val onDeckClicked: (Deck) -> Unit): FrameLayout(context) {
    val yourDecksAdapter = YourDecksAdapter()

    init {
        val recyclerView = RecyclerView(context)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        yourDecksAdapter.setOnClickListener{deckClickedId ->
            val deck = yourDecksAdapter.list.filter { it.id == deckClickedId }.firstOrNull()
            if (deck != null) {
                onDeckClicked(deck)
            }
        }
        recyclerView.adapter = yourDecksAdapter

        val spacing = 8.toPixel(context.resources.displayMetrics)

        setPadding(spacing, spacing, spacing, spacing)
        clipToPadding = false

        recyclerView.addItemDecoration(SpacesItemDecoration(spacing))

        addView(recyclerView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(PromoView(context), LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
}

fun Int.toPixel(displayMetrics: DisplayMetrics): Int {
    return (this.toFloat() * displayMetrics.density).toInt()
}

fun Int.toPixelFloat(displayMetrics: DisplayMetrics): Float {
    return (this.toFloat() * displayMetrics.density)
}