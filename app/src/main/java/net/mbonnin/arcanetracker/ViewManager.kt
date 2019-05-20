package net.mbonnin.arcanetracker

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.*
import com.crashlytics.android.Crashlytics
import java.util.*

/**
 * Created by martin on 10/21/16.
 */

class ViewManager(context: Context) {
    val width: Int
    val height: Int
    val usableWidth: Int
    val usableHeight: Int
    private val mWindowManager: WindowManager
    private val mViews = ArrayList<View>()

    fun addCenteredView(view: View, modal: Boolean = true) {
        val params = Params()

        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(wMeasureSpec, wMeasureSpec)

        params.x = (width - view.measuredWidth) / 2
        params.y = (height - view.measuredHeight) / 2
        params.w = view.measuredWidth
        params.h = view.measuredHeight

        if (modal) {
            addModalAndFocusableView(view, params)
        } else {
            addView(view, params)
        }
    }

    fun removeAllViewsExcept(view: View) {
        val it = mViews.iterator()
        while (it.hasNext()) {
            val view2 = it.next()
            if (view !== view2) {
                it.remove()
                mWindowManager.removeView(view2)
            }
        }
    }

    class Params {
        var x: Int = 0
        var y: Int = 0
        var w: Int = 0
        var h: Int = 0
    }

    init {
        mWindowManager = context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
        val screenSize = Point()

        mWindowManager.defaultDisplay.getSize(screenSize)
        if (screenSize.x > screenSize.y) {
            usableWidth = screenSize.x
            usableHeight = screenSize.y
        } else {
            usableHeight = screenSize.x
            usableWidth = screenSize.y
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWindowManager.defaultDisplay.getRealSize(screenSize)
            if (screenSize.x > screenSize.y) {
                width = screenSize.x
                height = screenSize.y
            } else {
                height = screenSize.x
                width = screenSize.y
            }
        } else {
            /**
             * best effort
             */
            width = usableWidth
            height = usableHeight
        }
    }

    fun addModalAndFocusableView(view: View, params: Params) {
        addModalViewInternal(view, params, 0)
    }

    fun addModalViewInternal(view: View, params: Params, extraFlags: Int) {
        addViewInternal(view, params, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or extraFlags)
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                removeView(view)
            }
            false
        }
    }

    fun addModalView(view: View, params: Params) {
        addModalViewInternal(view, params, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    private fun addViewInternal(view: View, params: Params, flags: Int) {
        if (mViews.contains(view)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Crashlytics.setBool("canDrawOverlay", android.provider.Settings.canDrawOverlays(view.context))
        }

        val layoutParams = WindowManager.LayoutParams(
                params.w,
                params.h,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                flags,
                PixelFormat.TRANSLUCENT)
        layoutParams.gravity = Gravity.TOP or Gravity.LEFT
        layoutParams.x = params.x
        layoutParams.y = params.y

        mWindowManager.addView(view, layoutParams)
        mViews.add(view)
    }

    fun addView(view: View, params: Params) {
        addViewInternal(view, params, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    fun removeView(view: View) {
        if (!mViews.contains(view)) {
            return
        }
        mViews.remove(view)
        mWindowManager.removeView(view)
    }

    fun updateView(view: View, r: Params) {
        if (!mViews.contains(view)) {
            return
        }

        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        layoutParams.width = r.w
        layoutParams.height = r.h
        layoutParams.x = r.x
        layoutParams.y = r.y

        mWindowManager.updateViewLayout(view, layoutParams)
    }

    fun addViewWithAnchor(view: View, x: Int, y: Int) {
        val params = ViewManager.Params()

        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(measureSpec, measureSpec)
        params.w = view.getMeasuredWidth()
        params.h = view.getMeasuredHeight()

        params.x = (x + Utils.dpToPx(20))
        params.y = (y - params.h / 2)
        if (params.y < 0) {
            params.y = 0
        } else if (params.y + params.h > ViewManager.Companion.get().height) {
            params.y = ViewManager.Companion.get().height - params.h
        }

        addView(view, params)
    }

    fun removeAllViews() {
        while (!mViews.isEmpty()) {
            val view = mViews[0]
            removeView(view)
        }
    }

    fun allViews(): List<View> {
        return mViews
    }

    fun addMenu(menuView: View, anchorView: View) {
        menuView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        menuView.measure(wMeasureSpec, wMeasureSpec)

        val a = IntArray(2)
        anchorView.getLocationOnScreen(a)

        val params = ViewManager.Params()
        params.x = a[0] + anchorView.width / 2
        params.y = a[1] + anchorView.height / 2 - menuView.measuredHeight
        params.w = menuView.measuredWidth
        params.h = menuView.measuredHeight

        addModalView(menuView, params)

    }

    companion object {
        var sViewManager: ViewManager? = null

        fun get(): ViewManager {
            if (sViewManager == null) {
                sViewManager = ViewManager(ArcaneTrackerApplication.context)
            }

            return sViewManager!!
        }
    }

}
