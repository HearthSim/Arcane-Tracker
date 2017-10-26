package net.mbonnin.arcanetracker.detector

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import timber.log.Timber

class RRectFactory(val screenWidth: Int, val screenHeight: Int, context: Context) {

    val isTablet: Boolean

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

    val ARENA_RECTS: Array<RRect>
        get() {
            return when {
                isPixelLike() -> ARENA_RECTS_PIXEL
                else ->
                    arrayOf(
                            scaleRect(RRect(344.138, 1080.0 - 642.198 - 187.951, 185.956, 187.951)),
                            scaleRect(RRect(854.205, 1080.0 - 642.198 - 187.951, 185.956, 187.951)),
                            scaleRect(RRect(1379.876, 1080.0 - 642.198 - 187.951, 185.956, 187.951))
                    )
            }
        }

    val FORMAT by lazy {
        when {
            isNexus9Like() -> FORMAT_NEXUS9
            isPixelLike() -> FORMAT_PIXEL
            else -> scaleRect(FORMAT_PIXEL)
        }
    }

    val RANK by lazy {
        when {
            isNexus9Like() -> RANK_NEXUS9
            isPixelLike() -> RANK_PIXEL
            else -> scaleRect(RANK_PIXEL)
        }
    }

    val MODE: RRect
        get() {
            return when {
                isNexus9Like() -> MODE_NEXUS9
                isPixelLike() -> MODE_PIXEL
                else -> scaleRect(MODE_PIXEL)
            }
        }

    private fun scaleRect(rRect: RRect): RRect {
        return rRect.scale(screenWidth.toDouble() / 1920.0, screenHeight.toDouble() / 1080.0)
    }

    companion object {
        val FORMAT_NEXUS9 = RRect(1609.0, 39.0, 96.0, 73.0)
        val FORMAT_PIXEL = RRect(1754.0, 32.0, 138.0, 98.0)

        val RANK_NEXUS9 = RRect(1730.0, 201.0, 110.0, 46.0)
        val RANK_PIXEL = RRect(820.0, 424.0, 230.0, 102.0)

        val MODE_NEXUS9 = RRect(1432.0, 400.0, 160.0, 34.0)
        val MODE_PIXEL = RRect(1270.0, 256.0, 140.0, 32.0)

        val ARENA_RECTS_PIXEL = arrayOf(
                RRect(344.138, 1080.0 - 642.198 - 187.951, 185.956, 187.951),
                RRect(854.205, 1080.0 - 642.198 - 187.951, 185.956, 187.951),
                RRect(1379.876, 1080.0 - 642.198 - 187.951, 185.956, 187.951)
        )

    }
}