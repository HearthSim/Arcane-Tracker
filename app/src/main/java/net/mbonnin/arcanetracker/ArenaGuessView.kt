package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class ArenaGuessView : View {
    private var cardId: String = "id"
    private var cardName: String = "name"
    private var score = -1f

    private val w: Int
    private val h: Int

    private val rect = RectF()

    private val paint = Paint()

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        this.w = dpToPx(150)
        this.h = dpToPx(50)


    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(),
                getContext().resources.displayMetrics).toInt()
    }


    fun setCardId(cardId: String, playerClass: String) {
        this.cardId = cardId
        this.cardName = CardUtil.getCard(cardId).name!!

        val card = CardUtil.getCard(cardId)
        card.scores?.let {
            for (heroScore in it) {
                if (playerClass.compareTo(heroScore.Hero, true) == 0) {
                    this.score = heroScore.Score
                }
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true

        paint.color = Color.argb(128, 0, 0, 0)

        rect.left = (h / 2).toFloat()
        rect.top = 0.toFloat()
        rect.right = w.toFloat()
        rect.bottom = h.toFloat()

        val radius = dpToPx(5).toFloat()
        canvas.drawRoundRect(rect, radius, radius, paint)

        //paint.typeface = ResourcesCompat.getFont(context, R.font.belwe_bold);

        paint.color = Color.WHITE
        paint.textSize = dpToPx(20).toFloat()
        paint.textAlign = Paint.Align.LEFT

        canvas.drawText(cardName, (h + dpToPx(5)).toFloat(), dpToPx(25).toFloat(), paint)

        val s = if (score < 0) "?" else score.toString()

        if (score < 0) {
            paint.color = Color.GRAY
        } else {
            paint.color = mixColors(Color.parseColor("#ff0000"), Color.parseColor("#00ff00"), score / 100)
        }

        canvas.drawCircle((h / 2).toFloat(), (h / 2).toFloat(), (h / 2).toFloat(), paint)

        val y = (h / 2).toFloat() - (paint.ascent()) / 2

        val textSize = dpToPx(30).toFloat()
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        canvas.drawText(s, (h / 2).toFloat(), y, paint)

        paint.textSize = textSize
        paint.strokeWidth = textSize * 0.01f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE

        canvas.drawText(s, (h / 2).toFloat(), y, paint)
    }

    private fun mixColors(c1: Int, c2: Int, a: Float): Int {
        return Color.argb(
                ((1 - a) * Color.alpha(c1) + a * Color.alpha(c2)).toInt(),
                ((1 - a) * Color.red(c1)   + a * Color.red(c2)).toInt(),
                ((1 - a) * Color.green(c1) + a * Color.green(c2)).toInt(),
                ((1 - a) * Color.blue(c1)  + a * Color.blue(c2)).toInt()
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(w, h)
    }
}