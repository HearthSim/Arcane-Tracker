package net.mbonnin.arcanetracker

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.toRect
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.reactivex.disposables.Disposable
import net.mbonnin.arcanetracker.helper.RarityHelper
import net.mbonnin.hsmodel.Card

class PackCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr), Target {
    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        bitmap = null
        invalidate()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        this.bitmap = bitmap
        invalidate()
    }

    private var disposable: Disposable? = null
    private val paint = Paint()
    private var bitmap: Bitmap? = null
    private var golden = false
    private val rect = RectF()
    private var glowPaint = Paint()

    val padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.resources.displayMetrics)
    val roundRectRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)
    private lateinit var card: Card

    init {
        // for blur
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        glowPaint.isAntiAlias = true
        glowPaint.style = Paint.Style.STROKE
        glowPaint.strokeWidth = 8.toPixelFloat(resources.displayMetrics)
        glowPaint.maskFilter = BlurMaskFilter(padding / 3, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, (20.toPixel(resources.displayMetrics) + 2 * padding).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        rect.top = 0f
        rect.left = 0f
        rect.right = width.toFloat()
        rect.bottom = height.toFloat()

        rect.inset(padding, padding)

        if (false && golden) {
            val a = 100 + 155 * (1 + Math.sin(2 * Math.PI * System.currentTimeMillis() / 5000)) / 2
            glowPaint.color = Color.argb(a.toInt(), 0xff, 0xd5, 0x4f)
            canvas.drawRoundRect(rect, roundRectRadius, roundRectRadius, glowPaint)
            invalidate()
        }

        if (bitmap != null && width > 0 && height > 0) {
            val croppedWidth: Int
            val croppedHeight: Int
            val y: Int
            val x: Int

            if (width/height > bitmap!!.width / bitmap!!.height) {
                croppedWidth = bitmap!!.width
                croppedHeight = croppedWidth * height / width
                x = 0
                y = (bitmap!!.height - croppedHeight) / 2
            } else {
                croppedHeight = bitmap!!.height
                croppedWidth = croppedHeight * width/height
                y = 0
                x = (bitmap!!.width - croppedWidth)/2
            }
            val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, croppedWidth, croppedHeight)
            val bitmapDrawable = RoundedBitmapDrawableFactory.create(context.resources, croppedBitmap)
            bitmapDrawable.cornerRadius = roundRectRadius
            bitmapDrawable.setAntiAlias(true)
            bitmapDrawable.setBounds(rect.toRect())
            bitmapDrawable.draw(canvas)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            canvas.drawRoundRect(rect, roundRectRadius, roundRectRadius, paint)
        }

        paint.style = Paint.Style.FILL
        if (golden) {
            paint.color = Color.argb(160, 0xff, 0xd5, 0x4f)
        } else {
            paint.color = Color.argb(100, 0, 0, 0)
        }
        canvas.drawRect(rect, paint)

        val rarityColor = RarityHelper.rarityToColor[card.rarity]
        if (rarityColor != null) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, context.resources.displayMetrics)
            paint.color = rarityColor

            canvas.drawRoundRect(rect, roundRectRadius, roundRectRadius, paint)
        }
    }

    fun setCard(card: Card, golden: Boolean) {
        this.card = card
        this.golden = golden

        disposable?.dispose()

        this.bitmap = null

        Picasso.with(context)
                .load("bar://" + card.id)
                .placeholder(R.drawable.hero_10)
                .into(this)
    }

    private var pressed_ = false

    private var detailsView: View? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed_ = true

                Picasso.with(context).load(Utils.getCardUrl(card.id)).into(object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
                        if (pressed_) {
                            detailsView = displayImageView(event.rawX, event.rawY, bitmap)
                        }
                    }

                    override fun onBitmapFailed(errorDrawable: Drawable?) {

                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

                    }
                })

            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                pressed_ = false
                if (detailsView != null) {
                    ViewManager.get().removeView(detailsView!!)
                    detailsView = null
                }
            }

        }

        return true
    }

    companion object {
        fun displayImageView(x: Float, y: Float, bitmap: Bitmap): View {
            val imageView = ImageView(ArcaneTrackerApplication.context)

            imageView.setImageBitmap(bitmap)

            val params = ViewManager.Params()
            params.h = (ViewManager.get().height / 1.5f).toInt()
            params.w = params.h * CardRenderer.TOTAL_WIDTH / CardRenderer.TOTAL_HEIGHT

            params.x = (x - Utils.dpToPx(80) - params.w).toInt()
            params.y = (y - params.h / 2).toInt()
            if (params.y < 0) {
                params.y = 0
            } else if (params.y + params.h > ViewManager.get().height) {
                params.y = ViewManager.get().height - params.h
            }
            if (params.x < 0) {
                params.x = (x + Utils.dpToPx(40)).toInt()
            }
            ViewManager.get().addModalView(imageView, params)

            return imageView
        }
    }
}