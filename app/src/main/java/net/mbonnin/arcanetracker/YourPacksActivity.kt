package net.mbonnin.arcanetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView


class YourPacksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recyclerView = RecyclerView(this)

        val adapter = YourPacksAdapter(this)

        val gridLayoutManager = GridLayoutManager(this, 6)
        gridLayoutManager.spanSizeLookup = adapter.spanSizeLookup

        recyclerView.layoutManager = gridLayoutManager

        val spacing = 8.toPixel(resources.displayMetrics)

        recyclerView.setPadding(spacing, spacing, spacing, spacing)
        recyclerView.clipToPadding = false

        recyclerView.adapter = adapter

        setContentView(recyclerView)
    }
}

