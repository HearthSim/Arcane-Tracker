package net.mbonnin.arcanetracker

import android.graphics.Color
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.CardJson
import java.util.ArrayList
import kotlin.Comparator

/**
 * Created by martin on 10/21/16.
 */
class CardsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var mClass: String? = null
    private val mCardList = ArrayList<Card>()
    private var mListener: Listener? = null
    private var mSearchQuery: String? = null
    private var mCost = -1
    private var mDisabledCards = ArrayList<String>()

    fun setCost(cost: Int) {
        mCost = cost
        filter()
    }

    fun setDisabledCards(list: ArrayList<String>) {
        mDisabledCards = list
        notifyDataSetChanged()
    }

    fun setListener(listener: Listener) {
        mListener = listener
    }

    interface Listener {
        fun onClick(card: Card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_view, null)

        val holder = object : RecyclerView.ViewHolder(view) {

        }

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        (imageView as AspectRatioImageView).setAspectRatio(1.51f)

        view.setOnTouchListener { v, event ->
            val position = holder.adapterPosition
            /**
             * the NO_POSITION case could happen if you click very fast.
             * not sure about the other case..., maybe it's not needed anymore
             */
            if (position == RecyclerView.NO_POSITION || position >= mCardList.size) {
                return@setOnTouchListener false
            }

            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (mDisabledCards.contains(mCardList[position].id)) {
                    return@setOnTouchListener false
                }
                imageView.setColorFilter(Color.argb(120, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
                return@setOnTouchListener true
            } else if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP) {
                imageView.clearColorFilter()

                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    mListener!!.onClick(mCardList[position])
                }
            }
            false
        }
        return holder
    }

    fun setClass(clazz: String) {
        mClass = clazz

        filter()
    }

    fun setSearchQuery(searchQuery: String) {
        if (searchQuery == "") {
            mSearchQuery = null
        } else {
            mSearchQuery = searchQuery.toLowerCase()
        }
        filter()

    }

    private fun filter() {
        mCardList.clear()
        val allCards = CardJson.allCards()

        for (card in allCards) {
            if (!card.collectible) {
                continue
            }

            if (card.cost == null) {
                continue
            }

            if (mCost != -1) {
                if (mCost == 7) {
                    if (card.cost ?: 0 < 7) {
                        continue
                    }
                } else if (card.cost != mCost) {
                    continue
                }
            }

            if (mClass != card.playerClass) {
                continue
            }

            if (mSearchQuery != null) {
                var found = false
                if (card.text != null && card.text!!.toLowerCase().contains(mSearchQuery!!)) {
                    found = true
                }

                if (!found && card.name.toLowerCase().contains(mSearchQuery!!)) {
                    found = true
                }

                if (!found && card.race != null && card.race!!.toLowerCase().contains(mSearchQuery!!)) {
                    found = true
                }

                if (!found) {
                    continue
                }
            }

            mCardList.add(card)
        }

        mCardList.sortWith(Comparator{ a, b ->
            val r =  Utils.compareNullSafe(a.cost, b.cost)
            if (r != 0) {
                return@Comparator r
            }
            a.name.compareTo(a.name)
        })

        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.imageView)
        val textView = holder.itemView.findViewById<TextView>(R.id.textView)

        val (id, _, _, _, _, name) = mCardList[position]

        if (mDisabledCards.contains(id)) {
            imageView.setColorFilter(Color.argb(180, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        } else {
            imageView.clearColorFilter()
        }

        val context = textView.context

        textView.text = name

        Picasso.with(context).load(Utils.getCardUrl(id)).into(imageView, object : Callback {
            override fun onSuccess() {
                textView.text = ""
            }

            override fun onError() {}
        })
    }

    override fun getItemCount(): Int {
        return mCardList.size
    }
}
