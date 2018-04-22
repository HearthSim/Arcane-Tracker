package net.mbonnin.arcanetracker

import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import net.mbonnin.arcanetracker.room.RDatabaseSingleton


class YourPacksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recyclerView = RecyclerView(this)

        val list = LivePagedListBuilder(RDatabaseSingleton.instance.packDao().all(),20).build()

        val adapter = PacksAdapter()
        list.observe(this, Observer {
            adapter.submitList(it)
        } )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setContentView(recyclerView)
    }
}

