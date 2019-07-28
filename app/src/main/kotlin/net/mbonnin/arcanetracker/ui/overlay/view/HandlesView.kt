package net.mbonnin.arcanetracker.ui.overlay.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import net.mbonnin.arcanetracker.ViewManager

class HandlesView : LinearLayout {
    private var mListener: OnTouchListener? = null

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    init {
        clipChildren = false
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

    fun setListener(listener: OnTouchListener) {
        mListener = listener
    }
}
