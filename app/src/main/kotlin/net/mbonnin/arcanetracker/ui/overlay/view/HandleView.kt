package net.mbonnin.arcanetracker.ui.overlay.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager

/**
 * Created by martin on 10/28/16.
 */

class HandleView : View {
    private var mTouchSlop: Int = 0
    private var mShadowSize: Int = 0
    private lateinit var mBlurPaint: Paint
    private lateinit var mGlowPaint: Paint
    private lateinit var mViewManager: ViewManager
    private lateinit var clipRect: Rect

    private var mParams: ViewManager.Params? = null
    private var mLayoutX: Int = 0
    private var mLayoutY: Int = 0
    private var mDownX: Float = 0.toFloat()
    private var mDownY: Float = 0.toFloat()
    private var hasMoved: Boolean = false

    private var mLastY: Int = 0
    private var mLastX: Int = 0
    private val mRadius: Int = 0
    private lateinit var mColorPaint: Paint
    private lateinit var mOverlayPaint: Paint
    private var mPressed: Boolean = false
    private var mDrawable: Drawable? = null
    private var glow: Boolean = false

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {}

    fun init(drawable: Drawable, color: Int) {
        mShadowSize = Utils.dpToPx(3)
        mViewManager = ViewManager.get()

        // for blur
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        mParams = ViewManager.Params()
        mParams!!.x = 0
        mParams!!.y = 0

        val vc = ViewConfiguration.get(context)
        mTouchSlop = vc.scaledTouchSlop

        mBlurPaint = Paint()
        mBlurPaint.isAntiAlias = true
        mBlurPaint.maskFilter = BlurMaskFilter(mShadowSize.toFloat(), BlurMaskFilter.Blur.NORMAL)
        mBlurPaint.color = Color.argb(150, 0, 0, 0)

        mColorPaint = Paint()
        mColorPaint.isAntiAlias = true
        mColorPaint.color = color

        mOverlayPaint = Paint()
        mOverlayPaint.isAntiAlias = true
        mOverlayPaint.color = Color.argb(128, 255, 255, 255)

        mGlowPaint = Paint()
        mGlowPaint.isAntiAlias = true
        mGlowPaint.style = Paint.Style.STROKE
        mGlowPaint.strokeWidth = mShadowSize.toFloat()

        mBlurPaint.color = Color.argb(150, 0, 0, 0)
        mDrawable = drawable

        clipRect = Rect()
    }


    fun show(show: Boolean) {
        if (show) {
            mLastX = mParams!!.x
            mLastY = mParams!!.y
            mViewManager.addView(this, mParams!!)
        } else {
            mViewManager.removeView(this)
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        // we reserve shadow size on each side
        val radius = measuredWidth/2 - mShadowSize

        var cx = measuredWidth.toFloat() / 2
        var cy = measuredHeight.toFloat() / 2

        if (mPressed) {
            cx += mShadowSize
            cy += mShadowSize
        }

        if (!mPressed) {
            // draw the shadow
            canvas.drawCircle(cx + mShadowSize.toFloat() / 2, cy + mShadowSize.toFloat() / 2, radius.toFloat(), mBlurPaint)
        }

        canvas.drawCircle(cx, cy, radius.toFloat(), mColorPaint)
        if (mDrawable != null) {
            mDrawable!!.setBounds(
                    (cx - radius).toInt(),
                    (cy - radius).toInt(),
                    (cx + radius).toInt(),
                    (cy + radius).toInt())
            mDrawable!!.draw(canvas)
        }

        if (mPressed) {
            canvas.drawCircle(cx, cy, radius.toFloat(), mOverlayPaint)

        }

        if (glow) {
            if (!mPressed) {
                val a = 120 + 135 * (1 + Math.sin(2 * Math.PI * System.currentTimeMillis() / 2000)) / 2
                mGlowPaint.color = Color.argb(a.toInt(), 0xff, 0xd5, 0x4f)
                canvas.drawCircle(cx, cy, (radius + mShadowSize / 2).toFloat(), mGlowPaint)
            }
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            mLayoutX = mParams!!.x
            mLayoutY = mParams!!.y
            mDownX = event.rawX
            mDownY = event.rawY
            hasMoved = false
            mPressed = true
            invalidate()
            return true
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (!hasMoved) {
                if (Math.hypot((event.rawX - mDownX).toDouble(), (event.rawY - mDownY).toDouble()) > mTouchSlop) {
                    hasMoved = true
                }
            }
            if (hasMoved) {
                mParams!!.x = (mLayoutX + (event.rawX - mDownX)).toInt()
                mParams!!.y = (mLayoutY + (event.rawY - mDownY)).toInt()

                mLastX = mParams!!.x
                mLastY = mParams!!.y

                mViewManager.updateView(this, mParams!!)
            }
        } else if (event.actionMasked == MotionEvent.ACTION_UP) {
            mPressed = false
            invalidate()
            if (!hasMoved) {
                performClick()
                return true
            }
        } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            mPressed = false
            invalidate()
        }
        return false
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredWidth)
    }

    fun glow(glow: Boolean) {
        this.glow = glow
        invalidate()
    }

    fun setColor(color: Int) {
        mColorPaint.color = color
    }
}
