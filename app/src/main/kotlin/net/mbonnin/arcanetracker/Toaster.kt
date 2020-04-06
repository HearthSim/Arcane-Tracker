package net.mbonnin.arcanetracker

import android.view.View
import timber.log.Timber

object Toaster {
    private var toastContainer: ToastContainer? = null

    fun show(view: View, durationMillis: Long = 5000): ToastContainer {
        if(toastContainer != null) {
            ViewManager.get().removeView(toastContainer!!)
        }

        val container = ToastContainer(ArcaneTrackerApplication.context)

        container.setToast(view, durationMillis) {
            dismiss(container)
        }

        container.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val width = container.measuredWidth
        val height = container.measuredHeight
        val params = ViewManager.Params().apply {
            x = (ViewManager.get().width - width).toInt()
            y = (ViewManager.get().height - height).toInt()
            w = width.toInt()
            h = height.toInt()
        }
        ViewManager.get().addView(container, params)

        toastContainer = container

        return container
    }

    fun dismiss(toastContainer: ToastContainer) {
        if (this.toastContainer == toastContainer) {
            ViewManager.get().removeView(toastContainer)
            this.toastContainer = null
        }
    }
}