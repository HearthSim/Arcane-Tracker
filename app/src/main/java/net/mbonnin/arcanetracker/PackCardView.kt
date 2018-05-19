package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.*
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.TypedValue
import android.view.View
import androidx.graphics.toRect
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.mbonnin.hsmodel.Card

class PackCardView(context: Context): View(context) {
    var disposable: Disposable? = null
    val paint = Paint()
    private var bitmap: Bitmap? = null
    private var golden = false
    val rect = RectF()

    val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, context.resources.displayMetrics)
    val roundRectRadius =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)

    override fun onDraw(canvas: Canvas) {
        rect.top = 0f
        rect.left = 0f
        rect.right = width.toFloat()
        rect.bottom = height.toFloat()

        rect.inset(padding, padding)

        if (bitmap != null) {
            val bitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, bitmap)
            bitmapDrawable.cornerRadius = roundRectRadius
            bitmapDrawable.setAntiAlias(true)
            bitmapDrawable.setBounds(rect.toRect())
            bitmapDrawable.draw(canvas)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            canvas.drawRoundRect(rect, roundRectRadius, roundRectRadius, paint)
        }

        if (golden) {
            paint.style = Paint.Style.STROKE
            paint.color = RarityHelper.GOLD_COLOR
            paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)
            canvas.drawRoundRect(rect, roundRectRadius, roundRectRadius, paint)
        }
    }

    private lateinit var card: Card


    fun setCard(card: Card, golden: Boolean) {
        this.card = card
        this.golden = golden

        disposable?.dispose()

        this.bitmap = null

        disposable = Single.fromCallable{
            val bitmap = Utils.getCardArtBlocking(card.id)
            if (bitmap == null) {
                throw Exception("cannot get bitmap")
            }
            bitmap
        }.subscribeOn(Schedulers.io())
                .subscribe( {
                    this.bitmap = it
                    invalidate()
                }, {
                    this.bitmap = null
                    invalidate()
                })
    }
}