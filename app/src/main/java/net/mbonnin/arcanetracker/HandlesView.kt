package net.mbonnin.arcanetracker

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout

class HandlesView : LinearLayout {
    val params: ViewManager.Params
    private var mListener: View.OnTouchListener? = null

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    init {
        params = ViewManager.Params()
        clipChildren = false
    }

    fun show(show: Boolean) {
        if (show) {
            ViewManager.get().addView(this, params)
        } else {
            ViewManager.get().removeView(this)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (mListener != null) {
            mListener!!.onTouch(this, ev)
        } else false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (mListener != null) {
            mListener!!.onTouch(this, ev)
        } else false
    }

    fun setListener(listener: View.OnTouchListener) {
        mListener = listener
    }

    fun update() {
        ViewManager.get().updateView(this, params)
    }
}
