package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import timber.log.Timber

class RRectFactory(val screenWidth: Int, val screenHeight: Int, context: Context) {

    var isTablet: Boolean

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getMetrics(dm)

        // we don't retrieve the screenWidth and screenHeight from the displayMnager as it may have the status/nav bars
        val diagonal = Math.hypot(screenWidth.toDouble() / dm.xdpi, screenHeight.toDouble() / dm.ydpi).toFloat()

        Timber.d("diagonal=" + diagonal)
        isTablet = diagonal >= 8
    }

    private fun isNexus9Like() = isTablet && screenWidth == 2048 && screenHeight == 1536
    private fun isPixelLike() = !isTablet && screenWidth == 1920 && screenHeight == 1080

    val ARENA_RECTS by lazy {
        when {
            isPixelLike() -> ARENA_RECTS_PIXEL
            else -> ARENA_RECTS_PIXEL.map(this::scalePhone).toTypedArray()
        }
    }

    val FORMAT by lazy {
        when {
            isNexus9Like() -> FORMAT_NEXUS9
            isPixelLike() -> FORMAT_PIXEL
            isTablet -> scaleTablet(FORMAT_NEXUS9)
            else -> scalePhone(FORMAT_PIXEL)
        }
    }

    val RANK by lazy {
        when {
            isNexus9Like() -> RANK_NEXUS9
            isPixelLike() -> RANK_PIXEL
            isTablet -> scaleTablet(RANK_NEXUS9)
            else -> scalePhone(RANK_PIXEL)
        }
    }

    val MODE by lazy {
        when {
            isNexus9Like() -> MODE_NEXUS9
            isPixelLike() -> MODE_PIXEL
            isTablet -> scaleTablet(MODE_NEXUS9)
            else -> scalePhone(MODE_PIXEL)
        }
    }

    private fun scalePhone(rRect: RRect): RRect {
        return scale(rRect, 1920.0, 1080.0)
    }

    private fun scaleTablet(rRect: RRect): RRect {
        return scale(rRect, 2048.0, 1536.0)
    }

    private fun scale(rRect: RRect, refWidth: Double, refHeight: Double): RRect {
        val scaledWidth: Double
        val scaledHeight: Double

        if (screenWidth.toDouble()/screenHeight > refWidth/refHeight) {
            scaledHeight = screenHeight.toDouble()
            scaledWidth = refWidth * scaledHeight/refHeight
        } else {
            scaledWidth = screenWidth.toDouble()
            scaledHeight = scaledWidth * refHeight / refWidth
        }

        val r1 = rRect.scale(scaledWidth/refWidth, scaledHeight/refHeight);

        return r1.translate((screenWidth - scaledWidth) / 2, (screenHeight - scaledHeight)/2)
    }


    companion object {
        val FORMAT_PIXEL = RRect(1754.0, 32.0, 138.0, 98.0)
        val FORMAT_NEXUS9 = RRect(1609.0, 39.0, 96.0, 73.0)

        val RANK_PIXEL = RRect(820.0, 424.0, 230.0, 102.0)
        val RANK_NEXUS9 = RRect(1730.0, 201.0, 110.0, 46.0)

        val MODE_PIXEL = RRect(1270.0, 256.0, 140.0, 32.0)
        val MODE_NEXUS9 = RRect(1432.0, 400.0, 160.0, 34.0)

        val RECTS_MINION_PIXEL = arrayOf(
                RRect(324.0, 258.0, 208.0, 208.0),
                RRect(844.0, 258.0, 208.0, 208.0),
                RRect(1364.0, 258.0, 208.0, 208.0)
        )

        val RECTS_SPELLS_PIXEL = arrayOf(
                RRect(331.0, 280.0, 189.0, 189.0),
                RRect(850.0, 280.0, 189.0, 189.0),
                RRect(1369.0, 280.0, 189.0, 189.0)
        )

        val RECTS_WEAPON_PIXEL = arrayOf(
                RRect(347.0, 270.0, 173.0, 173.0),
                RRect(870.0, 270.0, 173.0, 173.0),
                RRect(1391.0, 270.0, 173.0, 173.0)
        )

        val ARENA_RECTS_PIXEL = arrayOf(
                RRect(344.138, 1080.0 - 642.198 - 187.951, 185.956, 187.951),
                RRect(854.205, 1080.0 - 642.198 - 187.951, 185.956, 187.951),
                RRect(1379.876, 1080.0 - 642.198 - 187.951, 185.956, 187.951)
        )
    }
}