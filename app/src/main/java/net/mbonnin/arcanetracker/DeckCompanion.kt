package net.mbonnin.arcanetracker

import android.graphics.Color
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import timber.log.Timber

/**
 * Created by martin on 10/14/16.
 */

open class DeckCompanion(v: View) {
    internal val settings: ImageButton
    internal val winLoss: TextView
    internal val nodeck: View
    var deckName: TextView

    protected val recyclerView: RecyclerView
    protected val mViewManager: ViewManager

    private val mParams: ViewManager.Params
    private val mRecyclerViewParams: ViewManager.Params
    open var deck: Deck? = null
        set(value) {
            field = value

            if (value == null) {
                return
            }

            value.checkClassIndex()

            background.setImageDrawable(Utils.getDrawableForClassIndex(value.classIndex))
            deckName.text = value.name

            update()
        }
    private val background: ImageView

    private fun update() {
        nodeck.visibility = if (deck != null) View.GONE else View.VISIBLE

    }
    init {
        mViewManager = ViewManager.get()

        Timber.d("screen: " + mViewManager.width + "x" + mViewManager.height)

        val w = (0.33 * 0.5 * mViewManager.width.toDouble()).toInt()
        val h = mViewManager.height

        settings = v.findViewById(R.id.edit)
        winLoss = v.findViewById(R.id.winLoss)
        deckName = v.findViewById(R.id.deckName)
        background = v.findViewById(R.id.background)
        recyclerView = v.findViewById(R.id.recyclerView)
        nodeck = v.findViewById(R.id.nodeck)

        mParams = ViewManager.Params()
        mParams.x = 0
        mParams.y = 0
        mParams.w = w
        mParams.h = h

        mRecyclerViewParams = ViewManager.Params()
        mRecyclerViewParams.w = w
        mRecyclerViewParams.h = mViewManager.height - h

        recyclerView.setBackgroundColor(Color.BLACK)
        recyclerView.layoutManager = LinearLayoutManager(v.context)
    }
}

