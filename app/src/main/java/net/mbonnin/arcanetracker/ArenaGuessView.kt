package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.support.v4.content.res.ResourcesCompat
import android.text.TextPaint
import android.text.TextUtils
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
    val textPaint = TextPaint()

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
        this.w = dpToPx(180)
        this.h = dpToPx(40)


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
                val hero2 = heroScore.Hero
                if (hero2 != null && playerClass.compareTo(hero2, true) == 0) {
                    this.score = heroScore.Score
                }
            }
            if (this.score < 0) {
                for (heroScore in it) {
                    val hero2 = heroScore.Hero
                    if (hero2 == null) {
                        this.score = heroScore.Score
                    }
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


        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true
        textPaint.textSize = dpToPx(15).toFloat()
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = ResourcesCompat.getFont(context, R.font.belwe_bold);


        val x = (h + dpToPx(5)).toFloat()
        val ellipsized = TextUtils.ellipsize(cardName, textPaint, w - x - dpToPx(5), TextUtils.TruncateAt.END)
        canvas.drawText(ellipsized.toString(), x, dpToPx(25).toFloat(), textPaint)

        val s = if (score < 0) "?" else score.toInt().toString()

        paint.color = scoreColor(score)

        canvas.drawCircle((h / 2).toFloat(), (h / 2).toFloat(), (h / 2).toFloat(), paint)

        val y = (h / 2).toFloat() - (textPaint.ascent()) / 2

        val textSize = dpToPx(30).toFloat()
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL

        canvas.drawText(s, (h / 2).toFloat(), y, textPaint)

        textPaint.textSize = textSize
        textPaint.strokeWidth = textSize * 0.02f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.BLACK
        textPaint.style = Paint.Style.STROKE

        canvas.drawText(s, (h / 2).toFloat(), y, textPaint)
    }

    private val hsv = FloatArray(3)

    private fun scoreColor(score: Float): Int {
        var step: Int
        when {
            score < 68 -> step = 0
            score < 86 -> step = 1
            score < 95 -> step = 2
            score < 104 -> step = 3
            score < 113 -> step = 4
            score < 131 -> step = 5
            score < 300 -> step = 6
            else -> return Color.GRAY
        }

        hsv[0] = step * 120.0f/6
        hsv[1] = 0.78f
        hsv[2] = 0.80f

        return Color.HSVToColor(hsv)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(w, h)
    }
}