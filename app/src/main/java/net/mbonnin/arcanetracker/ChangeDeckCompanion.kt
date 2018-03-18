package net.mbonnin.arcanetracker

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View

class ChangeDeckCompanion(icon: View, anchor: View, callback: () -> Unit) {
    init {
        val viewManager = ViewManager.get()

        val a = IntArray(2)
        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        anchor.getLocationOnScreen(a)

        icon.setOnClickListener({ v2 ->
            callback()

            val deckListView = LayoutInflater.from(v2.getContext()).inflate(R.layout.decklist_view, null)
            val recyclerView = deckListView.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(v2.getContext())
            val adapter = DeckListAdapter()
            adapter.setOnDeckSelectedListener { deck ->
                viewManager.removeView(deckListView)
                MainViewCompanion.legacyCompanion.deck = deck
            }
            recyclerView.adapter = adapter

            deckListView.measure(wMeasureSpec, wMeasureSpec)

            var h = deckListView.measuredHeight
            if (h > viewManager.height) {
                h = viewManager.height
            }
            val params = ViewManager.Params()
            params.x = a[0] + anchor.width / 2 + Utils.dpToPx(20)
            params.y = a[1] + anchor.height / 2 - h
            params.w = deckListView.measuredWidth
            params.h = h

            viewManager.addModalView(deckListView, params)
        })
    }

}