package net.mbonnin.arcanetracker

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView


class YourPacksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recyclerView = RecyclerView(this)

        val adapter = YourPacksAdapter(this)

        val gridLayoutManager = GridLayoutManager(this, 6)
        gridLayoutManager.spanSizeLookup = adapter.spanSizeLookup

        recyclerView.layoutManager = gridLayoutManager

        recyclerView.adapter = adapter

        setContentView(recyclerView)
    }
}

