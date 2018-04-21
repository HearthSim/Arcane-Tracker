package net.mbonnin.arcanetracker.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.Rarity
import timber.log.Timber

internal class DeckEntryHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnTouchListener {
    private val mHandler: Handler
    var gift: ImageView
    var background: ImageView
    var cost: TextView
    var name: TextView
    var count: TextView
    var overlay: View

    lateinit var card: Card
    private var detailsView: DetailsView? = null

    private var bitmap: Bitmap? = null
    private var longPress: Boolean = false

    private val mLongPressRunnable = Runnable {
        longPress = true
        displayImageViewIfNeeded()
    }
    private var downY: Float = 0.toFloat()
    private var downX: Float = 0.toFloat()
    private var deckEntry: DeckEntryItem? = null

    private fun displayImageViewIfNeeded() {
        if (longPress &&
                ("?" == card.id || bitmap != null)
                && deckEntry != null) {
            if (detailsView != null) {
                Timber.d("too many imageViews")
                return
            }

            detailsView = DetailsView(itemView.context)

            /*
             * bitmap might be null if the card comes from the Hand
             */
            detailsView!!.configure(bitmap, deckEntry!!, (ViewManager.get().height / 1.5f).toInt())

            val params = ViewManager.Params()

            val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            detailsView!!.measure(measureSpec, measureSpec)
            if (detailsView!!.measuredHeight >= ViewManager.get().height) {
                detailsView!!.setTopMargin(0)
                detailsView!!.measure(measureSpec, measureSpec)
            }
            params.w = detailsView!!.measuredWidth
            params.h = detailsView!!.measuredHeight

            params.x = (downX + Utils.dpToPx(40)).toInt()
            params.y = (downY - params.h / 2).toInt()
            if (params.y < 0) {
                params.y = 0
            } else if (params.y + params.h > ViewManager.get().height) {
                params.y = ViewManager.get().height - params.h
            }
            ViewManager.get().addModalView(detailsView!!, params)
        }

    }

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
                .load("bar://" + card.id)
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

        resetImageView()

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

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.rawX
            downY = event.rawY
            if ("?" != card.id) {
                Picasso.with(v.context).load(Utils.getCardUrl(card.id)).into(object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
                        this@DeckEntryHolder.bitmap = bitmap
                        displayImageViewIfNeeded()
                    }

                    override fun onBitmapFailed(errorDrawable: Drawable) {

                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable) {

                    }
                })
            }
            mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP) {
            resetImageView()
        }

        return true
    }

    private fun resetImageView() {
        if (detailsView != null) {
            ViewManager.get().removeView(detailsView!!)
            detailsView = null
        }
        mHandler.removeCallbacksAndMessages(null)
        longPress = false
        bitmap = null
    }
}
