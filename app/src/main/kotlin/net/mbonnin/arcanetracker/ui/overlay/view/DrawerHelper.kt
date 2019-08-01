package net.mbonnin.arcanetracker.ui.overlay.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import net.mbonnin.arcanetracker.Utils
import net.mbonnin.arcanetracker.ViewManager
import net.mbonnin.arcanetracker.ui.overlay.Onboarding
import timber.log.Timber

class DrawerHelper(private val view: View, private val handles: HandlesView, private val edge: Edge) : ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private var mOffset: Int = 0
    private var maxOffset = 0

    private var mHandlesMovement: Int = 0

    private var direction = 1
    private var velocityRef: Float = 0f
    private var velocityLast: Float = 0f
    private var velocityRefTime: Long = 0

    private val mAnimator: ValueAnimator = ValueAnimator()
    private val mHandler = Handler()
    private var mRefY: Float = 0f
    private var mRefX: Float = 0f
    private val mTouchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
    private var mDownY: Float = 0f
    private var mDownX: Float = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private val mPadding = Utils.dpToPx(5)
    private var buttonWidth = 0

    private val mViewManager = ViewManager.get()
    private val viewParams = ViewManager.Params()
    private val handlesParams = ViewManager.Params()

    private val mHideViewRunnable = {
        Timber.d("$edge: hideView")
        when (edge) {
            Edge.LEFT -> viewParams.w = 0
            Edge.TOP -> viewParams.h = 0
        }
        mViewManager.updateView(view, viewParams)
    }

    private val mHandlesViewTouchListener = View.OnTouchListener { _, ev ->
        val raw: Float
        val down: Float
        val ref: Float
        val rawOther: Float
        val downOther: Float
        val refOther: Float
        val movement: Int

        when (edge) {
            Edge.LEFT -> {
                raw = ev.rawX
                ref = mRefX
                down = mDownX
                rawOther = ev.rawY
                refOther = mRefY
                downOther = mDownY
                movement = HANDLES_MOVEMENT_X
            }
            else -> {
                raw = ev.rawY
                ref = mRefY
                down = mDownY
                rawOther = ev.rawX
                refOther = mRefX
                downOther = mDownX
                movement = HANDLES_MOVEMENT_Y
            }
        }

        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getRawX()
            mDownY = ev.getRawY()
            mRefX = handlesParams.x.toFloat()
            mRefY = handlesParams.y.toFloat()
            mHandlesMovement = 0

        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mHandlesMovement == 0) {
                if (Math.abs(ev.getRawX() - mDownX) > mTouchSlop) {
                    mHandlesMovement = HANDLES_MOVEMENT_X
                } else if (Math.abs(ev.getRawY() - mDownY) > mTouchSlop) {
                    mHandlesMovement = HANDLES_MOVEMENT_Y
                }

                if (mHandlesMovement == movement) {
                    prepareAnimation()

                    velocityRef = raw
                    velocityLast = raw
                    velocityRefTime = System.nanoTime()
                }
            }

            if (mHandlesMovement == movement) {
                if ((raw - velocityLast) * direction > 0) {
                    velocityLast = raw
                } else {
                    direction = -direction
                    velocityRef = raw
                    velocityLast = raw
                    velocityRefTime = System.nanoTime()
                }
                var o = (ref + raw - down).toInt()
                if (o > maxOffset) {
                    o = maxOffset
                } else if (o < 0) {
                    o = 0
                }
                setOffset(o)
            } else if (mHandlesMovement != 0) {
                val o = (refOther + rawOther - downOther).toInt()
                when (edge) {
                    Edge.LEFT -> handlesParams.y = o
                    Edge.TOP -> handlesParams.x = o
                }
                mViewManager.updateView(handles, handlesParams)
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL || ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mHandlesMovement == movement) {
                var velocity = 0f
                val timeDiff = System.nanoTime() - velocityRefTime
                if (timeDiff > 0) {
                    velocity = 1000f * 1000f * (raw - velocityRef) / timeDiff
                }
                Timber.w("velocity: %f", velocity)
                if (mOffset < maxOffset) {
                    if (velocity <= 0) {
                        animateOffsetTo(0, velocity)
                    } else if (velocity > 0) {
                        animateOffsetTo(maxOffset, velocity)
                    }
                }
            }
        }

        mHandlesMovement != 0
    }

    init {
        handles.setListener(mHandlesViewTouchListener)

        mAnimator.addUpdateListener(this)
        mAnimator.addListener(this)
    }

    fun setButtonWidth(buttonWidth: Int) {
        this.buttonWidth = buttonWidth

        val wMeasureSpec = View.MeasureSpec.makeMeasureSpec(buttonWidth, View.MeasureSpec.EXACTLY)
        val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        handles.measure(wMeasureSpec, hMeasureSpec)

        handlesParams.w = handles.measuredWidth
        handlesParams.h = handles.measuredHeight
        mViewManager.updateView(handles, handlesParams)

    }

    fun setAlpha(progress: Int) {
        val a = 0.5f + progress / 200f
        view.alpha = a
        handles.alpha = a
    }

    private fun prepareAnimation() {
        mHandler.removeCallbacks(mHideViewRunnable)
        viewParams.w = viewWidth
        viewParams.h = viewHeight
        mViewManager.updateView(view, viewParams)
    }

    private fun animateOffsetTo(targetOffset: Int, pixelPerMillisecond: Float) {
        var pxPerMs = pixelPerMillisecond
        pxPerMs = Math.abs(pxPerMs)
        if (pxPerMs < 0.6) {
            pxPerMs = 0.6f
        }
        mAnimator.cancel()

        mAnimator.interpolator = LinearInterpolator()
        if (pxPerMs > 0) {
            mAnimator.duration = (Math.abs(mOffset - targetOffset) / pxPerMs).toLong()
        } else {
            mAnimator.duration = 300
        }

        prepareAnimation()
        mAnimator.setIntValues(mOffset, targetOffset)
        mAnimator.start()
    }

    private fun animateOffsetTo(targetOffset: Int) {
        mAnimator.cancel()
        mAnimator.interpolator = AccelerateDecelerateInterpolator()
        mAnimator.duration = 300

        prepareAnimation()
        mAnimator.setIntValues(mOffset, targetOffset)
        mAnimator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        setOffset(animation.animatedValue as Int)
    }

    private fun setOffset(offset: Int) {
        Timber.w("${edge}: setOffset: %d", mOffset);
        mOffset = offset
        when (edge) {
            Edge.LEFT -> {
                view.translationX = (-viewWidth + mOffset).toFloat()
                handlesParams.x = mOffset + mPadding
            }
            Edge.TOP -> {
                view.translationY = (-viewParams.h + mOffset).toFloat()
                handlesParams.y = mOffset + mPadding
            }
        }

        mViewManager.updateView(handles, handlesParams)

        Onboarding.updateTranslation()
    }

    override fun onAnimationStart(animation: Animator) {

    }

    override fun onAnimationEnd(animation: Animator) {
        //Timber.w("onAnimationEnd: %d", mOffset);
        if (mOffset == 0) {
            /**
             * XXX: somehow if I do this too early, there a small glitch on screen...
             */
            mHandler.postDelayed(mHideViewRunnable, 300)
        }
    }

    override fun onAnimationCancel(animation: Animator) {

    }

    override fun onAnimationRepeat(animation: Animator) {

    }

    fun show(show: Boolean) {
        if (show) {
            mViewManager.addView(view, viewParams)
            mViewManager.addView(handles, handlesParams)
        } else {
            mViewManager.removeView(view)
            mViewManager.removeView(handles)
        }
    }

    fun setViewWidth(width: Int) {
        Timber.d("setViewWidth($width)")

        viewWidth = width

        when (edge) {
            Edge.LEFT -> {
                mAnimator.cancel()
                maxOffset = width
                // update the handles
                if (mOffset != 0) {
                    viewParams.w = width
                    setOffset(maxOffset)
                }
            }
            Edge.TOP -> {
                viewParams.x = (mViewManager.width - viewWidth) / 2
                handlesParams.x = (mViewManager.width - handlesParams.w) / 2
                mViewManager.updateView(handles, handlesParams)
            }
        }

        mViewManager.updateView(view, viewParams)
    }

    fun setViewHeight(height: Int) {
        Timber.d("setViewHeight($height)")
        viewHeight = height

        when (edge) {
            Edge.LEFT -> {
                viewParams.h = height
                handlesParams.y = ViewManager.get().height - handlesParams.h - Utils.dpToPx(50)
                mViewManager.updateView(handles, handlesParams)
            }
            Edge.TOP -> {
                mAnimator.cancel()
                maxOffset = height
                // update the handles
                if (mOffset != 0) {
                    viewParams.h = height
                    setOffset(maxOffset)
                }
            }
        }
        mViewManager.updateView(view, viewParams)
    }

    fun isOpen(): Boolean {
        return mOffset == maxOffset
    }

    fun open() {
        Timber.d("$edge: open")
        animateOffsetTo(maxOffset)
    }

    fun close() {
        Timber.d("$edge: close")
        animateOffsetTo(0)
    }

    fun notifyHandlesChanged(resetY: Boolean = false) {
        setButtonWidth(buttonWidth)
        if (resetY) {
            handlesParams.y = ViewManager.get().height - handlesParams.h - Utils.dpToPx(50)
            mViewManager.updateView(handles, handlesParams)
        }
    }

    enum class Edge {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }

    companion object {
        private val HANDLES_MOVEMENT_X = 1
        private val HANDLES_MOVEMENT_Y = 2
    }

}