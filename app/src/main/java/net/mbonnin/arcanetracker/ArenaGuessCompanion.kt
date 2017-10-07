package net.mbonnin.arcanetracker

import android.view.View


object ArenaGuessCompanion {
    val views: Array<ArenaGuessView?> = Array(3, {null})

    fun show(index: Int, cardId: String, playerClass: String) {
        hide(index)

        views[index] = ArenaGuessView(ArcaneTrackerApplication.getContext())
        views[index]?.setCardId(cardId, playerClass)

        val params = ViewManager.Params()
        val w = ViewManager.get().width
        val h = ViewManager.get().height

        val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        views[index]?.measure(measureSpec, measureSpec)

        params.x = (180 + index * 1563/3) * w / 1920
        params.y = 30 * w / 1080
        params.w = views[index]?.measuredWidth!!
        params.h = views[index]?.measuredHeight!!

        ViewManager.get().addView(views[index], params)
    }

    fun hide(index: Int) {
        if (views[index] != null) {
            ViewManager.get().removeView(views[index])
            views[index] = null
        }
    }

    fun hideAll() {
        for (i in 0 until views.size) {
            hide(i)
        }
    }

}