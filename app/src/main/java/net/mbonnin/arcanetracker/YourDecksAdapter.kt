package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import timber.log.Timber

class YourDecksAdapter : RecyclerView.Adapter<YourDecksAdapter.ViewHolder>() {
    val list = mutableListOf<Deck>()

    init {
        RDatabaseSingleton.instance.deckDao().getAll()
                .subscribeOn(Schedulers.io())
                .map {rdeckList  ->
                    rdeckList.map { rDeck ->
                        DeckMapper.fromRDeck(rDeck)
                    }
                            .filterNotNull()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                    list.addAll(it)
                    notifyDataSetChanged()
                }, Timber::e)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_deck, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val deck = list[position]

        holder.deckBackground.setImageDrawable(Utils.getDrawableForClassIndex(deck.classIndex))
        holder.deckName.setText(deck.name)
    }

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val deckBackground = view.findViewById<ImageView>(R.id.deckBackground)
        val deckName = view.findViewById<TextView>(R.id.deckName)
    }
}

