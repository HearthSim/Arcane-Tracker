package net.mbonnin.arcanetracker.ui.overlay.adapter

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import net.hearthsim.hslog.DeckEntry
import net.hearthsim.hslog.parser.power.BattlegroundsBoard
import net.hearthsim.hsmodel.Card
import net.mbonnin.arcanetracker.ArcaneTrackerApplication
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager

internal class HeroHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnTouchListener {
    private lateinit var hero: DeckEntry.Hero
    private val mHandler: Handler
    var background: ImageView
    var name: TextView

    lateinit var card: Card

    private var downY: Float = 0.toFloat()
    private var downX: Float = 0.toFloat()

    init {
        val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30))
        itemView.layoutParams = params
        background = itemView.findViewById(R.id.background)
        name = itemView.findViewById(R.id.name)

        mHandler = Handler()
        itemView.setOnTouchListener(this)
    }

    fun bind(hero: DeckEntry.Hero) {
        this.card = hero.card

        Picasso.with(itemView.context)
                .load(Utils.getTileUrl(card.id))
                .placeholder(R.color.black)
                .into(background)

        name.text = card.name

        this.hero = hero
    }

    private var detailsView: View? = null

    private var pressed = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.rawX
            downY = event.rawY
            pressed = true
            detailsView = displayImageView(downX, downY, hero.board)
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
        fun displayImageView(x: Float, y: Float, board: BattlegroundsBoard): View {
            val boardView = BattlegroundsBoardView(ArcaneTrackerApplication.context)

            val params = ViewManager.Params()

            params.x = (x + Utils.dpToPx(100)).toInt()
            params.y = 0

            params.w = ViewManager.get().width - params.x
            params.h = ViewManager.get().height

            ViewManager.get().addModalView(boardView, params)

            boardView.configure(board)

            return boardView
        }
    }
}
