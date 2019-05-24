package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.graphics.Color
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager
import net.mbonnin.arcanetracker.hslog.power.Entity
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.enum.Rarity

internal class DeckEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnTouchListener {
    private val mHandler: Handler
    var gift: ImageView
    var background: ImageView
    var cost: TextView
    var name: TextView
    var count: TextView
    var overlay: View

    lateinit var card: Card

    private var downY: Float = 0.toFloat()
    private var downX: Float = 0.toFloat()
    private var deckEntry: DeckEntryItem? = null

    init {
        val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30))
        itemView.layoutParams = params
        background = itemView.findViewById(R.id.background)
        cost = itemView.findViewById(R.id.cost)
        name = itemView.findViewById(R.id.name)
        count = itemView.findViewById(R.id.count)
        overlay = itemView.findViewById(R.id.overlay)
        gift = itemView.findViewById(R.id.gift)

        mHandler = Handler()
        itemView.setOnTouchListener(this)
    }


    fun bind(entry: DeckEntryItem) {
        this.card = entry.card
        val c = entry.count

        Picasso.with(itemView.context)
                .load(Utils.getTileUrl(card.id))
                .placeholder(R.drawable.hero_10)
                .into(background)

        val costInt = Utils.valueOf(card.cost)
        if (costInt >= 0) {
            cost.text = costInt.toString() + ""
            cost.visibility = View.VISIBLE
        } else {
            cost.visibility = View.GONE
        }
        name.text = card.name
        count.visibility = GONE

        if (c > 0) {
            overlay.setBackgroundColor(Color.TRANSPARENT)
        } else {
            overlay.setBackgroundColor(Color.argb(150, 0, 0, 0))
        }

        if (entry.gift) {
            gift.visibility = View.VISIBLE
        } else {
            gift.visibility = GONE
        }

        if (c > 1) {
            count.visibility = View.VISIBLE
            count.text = c.toString() + ""
        } else if (c == 1 && Rarity.LEGENDARY == card.rarity) {
            count.visibility = View.VISIBLE
            count.text = "\u2605"
        } else {
            count.visibility = GONE
        }

        deckEntry = entry
    }

    private var detailsView: View? = null

    private var pressed = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.rawX
            downY = event.rawY
            pressed = true
            detailsView = displayImageView(downX, downY, card.id, deckEntry?.entityList ?: listOf())
        } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            pressed = false
            if (detailsView != null) {
                ViewManager.get().removeView(detailsView!!)
                detailsView = null
            }
        }

        return true
    }

    companion object {
        fun displayImageView(x: Float, y: Float, cardId: String, entityList: List<Entity>): View {
            val detailsView = DetailsView(ArcaneTrackerApplication.context)

            detailsView.configure(cardId, entityList, y.toInt())

            val params = ViewManager.Params()

            params.x = (x + Utils.dpToPx(40)).toInt()
            params.y = 0

            params.w = ViewManager.get().width - params.x
            params.h = ViewManager.get().height

            ViewManager.get().addModalView(detailsView, params)

            return detailsView
        }
    }
}
