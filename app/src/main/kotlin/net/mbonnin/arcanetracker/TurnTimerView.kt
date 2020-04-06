package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class TurnTimerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var opponent = ""
    var turn = ""
    var player = ""
    val bounds = Rect()


    val displayMetrics = context.resources.displayMetrics
    val eightDps = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, displayMetrics)

    companion object {
        val monospaceBold = Typeface.create("monospace", Typeface.BOLD)
    }

    val blackPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        typeface = monospaceBold
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, displayMetrics)
        isAntiAlias = true
    }
    val whitePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        typeface = monospaceBold
        isAntiAlias = true
    }

    fun setValues(opponent: String, turn: String, player: String) {
        this.opponent = opponent
        this.turn = turn
        this.player = player
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, displayMetrics)

        var height = 0f

        height += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, displayMetrics)

        whitePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, displayMetrics)
        var fm = whitePaint.fontMetrics
        height += fm.bottom - fm.top + fm.leading

        whitePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, displayMetrics)
        fm = whitePaint.fontMetrics
        height += fm.bottom - fm.top + fm.leading

        whitePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, displayMetrics)
        fm = whitePaint.fontMetrics
        height += fm.bottom - fm.top + fm.leading

        height += TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, displayMetrics)

        setMeasuredDimension(width.toInt(), height.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        var y = 0f
        var x = 0f

//        canvas.drawRect(Rect(0, 0, width, height), Paint().apply {
//            color = Color.RED
//        })
//
//        Timber.d("draw=$opponent turn=$turn player=$player")

        blackPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, displayMetrics)
        whitePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, displayMetrics)

        whitePaint.getTextBounds(turn, 0, turn.length, bounds)
        var w = whitePaint.measureText(turn)

        canvas.drawText(turn, width - eightDps - w, (height / 2 + bounds.height() / 2).toFloat(), blackPaint)
        canvas.drawText(turn, width - eightDps - w, (height / 2 + bounds.height() / 2).toFloat(), whitePaint)

        blackPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, displayMetrics)
        whitePaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, displayMetrics)

        whitePaint.getTextBounds(opponent, 0, opponent.length, bounds)
        w = whitePaint.measureText(opponent)

        canvas.drawText(opponent, width - eightDps - w, (height / 4 + bounds.height() / 2).toFloat(), blackPaint)
        canvas.drawText(opponent, width - eightDps - w, (height / 4 + bounds.height() / 2).toFloat(), whitePaint)

        whitePaint.getTextBounds(player, 0, player.length, bounds)
        w = whitePaint.measureText(player)

        canvas.drawText(player, width - eightDps - w, (3 * height / 4 + bounds.height() / 2).toFloat(), blackPaint)
        canvas.drawText(player, width - eightDps - w, (3 * height / 4 + bounds.height() / 2).toFloat(), whitePaint)
    }
}