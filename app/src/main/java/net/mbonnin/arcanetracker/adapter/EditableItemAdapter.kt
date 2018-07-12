package net.mbonnin.arcanetracker.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.Deck
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.hsmodel.enum.Rarity
import java.util.*

/**
 * Created by martin on 10/21/16.
 */
class EditableItemAdapter : ItemAdapter() {
    private var mDeck: Deck? = null

    val disabledCards: ArrayList<String>
        get() {
            val list = ArrayList<String>()
            if (!mDeck!!.isArena) {
                for (o in super.list) {
                    val deckEntryItem = o as DeckEntryItem
                    if (deckEntryItem.count == 2) {
                        list.add(deckEntryItem.card.id)
                    } else if (deckEntryItem.count == 1 && Rarity.LEGENDARY == deckEntryItem.card.rarity) {
                        list.add(deckEntryItem.card.id)
                    }
                }
            }

            return list
        }

    fun setDeck(deck: Deck) {
        mDeck = deck

        update()
    }

    fun addCard(cardId: String) {
        Utils.cardMapAdd(mDeck!!.cards, cardId, 1)
        update()
    }

    private fun update() {
        val list = ArrayList<Any>()
        for ((key, value) in mDeck!!.cards) {
            list.add(DeckEntryItem(card = CardUtil.getCard(key), count = value))
        }

        Collections.sort(list) { a, b -> Utils.compareNullSafe((a as DeckEntryItem).card.cost, (b as DeckEntryItem).card.cost) }

        setList(list)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)

        if (viewType == ItemAdapter.TYPE_DECK_ENTRY) {
            holder!!.itemView.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    holder.itemView.findViewById<View>(R.id.overlay).setBackgroundColor(Color.argb(150, 255, 255, 255))
                } else if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP) {
                    holder.itemView.findViewById<View>(R.id.overlay).setBackgroundColor(Color.TRANSPARENT)
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        val position = holder.adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val entry = list[position] as DeckEntryItem
                            mDeck!!.addCard(entry.card.id, -1)
                            update()
                        }
                    }
                }
                true
            }
        }
        return holder
    }
}
