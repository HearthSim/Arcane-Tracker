package net.mbonnin.arcanetracker

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import kotlinx.coroutines.*

class ToastContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {
    private var job: Job? = null

    init {
        val padding= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics).toInt()
        setPadding(0, 0, padding, padding)
        clipToPadding = false
        //setBackgroundColor(Color.parseColor("#80FF0000"))
    }

    fun setToast(view: View, durationMillis: Long, onDismissed: () -> Unit) {
        removeAllViews()
        addView(view, MATCH_PARENT, MATCH_PARENT)

        viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.translationY = this@ToastContainer.height.toFloat()
                view.animate().translationY(0f).start()
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        job = GlobalScope.launch(Dispatchers.Main) {
            delay(durationMillis)
            view.animate().translationX(this@ToastContainer.width.toFloat()).start()
            // Small hack to not add yet another animation listener
            delay(view.animate().duration)
            onDismissed()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job?.cancel()
    }
}