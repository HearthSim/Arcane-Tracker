package net.mbonnin.arcanetracker

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Flowable.zip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.deck_line_view.*
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RDeck

class PlayerDeckListAdapter : DeckListAdapter() {
    private var listener: ((deck: Deck) -> Unit)? = null
    private var list = mutableListOf<RDeck>()
    private var whizbangDeck: Deck? = null

    init {
        zip(
                RDatabaseSingleton.instance.deckDao().getCollection(),
                RDatabaseSingleton.instance.deckDao().getLatestArenaDeck(),
                object : BiFunction<List<RDeck>, List<RDeck>, List<RDeck>> {
                    override fun apply(t1: List<RDeck>, t2: List<RDeck>): List<RDeck> {
                        val r = t1.toMutableList()
                        r.addAll(t2)
                        return r
                    }
                }
        ).firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    list.addAll(it)
                    notifyDataSetChanged()
                }
    }

    override fun setOnDeckSelectedListener(listener: (deck: Deck) -> Unit) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(ArcaneTrackerApplication.context).inflate(R.layout.deck_line_view, null)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size + if (whizbangDeck == null) 0 else 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < list.size) {
            val rdeck = list[position]

            (holder as ViewHolder).bind(rdeck)
        } else {
            (holder as ViewHolder).bind(whizbangDeck)
        }
    }

    fun setWhizbangDeck(whizbangDeck: Deck) {
        this.whizbangDeck = whizbangDeck
    }

    inner class ViewHolder(override val containerView: View) : LayoutContainer, RecyclerView.ViewHolder(containerView) {
        fun bind(rdeck: RDeck) {
            bind(DeckMapper.fromRDeck(rdeck))
        }

        fun bind(deck: Deck?) {
            itemView.setOnClickListener { v ->
                if (deck != null) {
                    listener?.invoke(deck)
                }
            }

            deckImageRound.setImageDrawable(Utils.getDrawableForNameDeprecated(String.format("hero_%02d_round", (deck?.classIndex ?:0) + 1)))
            deckName.text = deck?.name
        }
    }

    companion object {
        var instance: PlayerDeckListAdapter? = null
        fun get(): PlayerDeckListAdapter {
            if (instance == null) {
                instance = PlayerDeckListAdapter()
            }
            return instance!!
        }
    }
}