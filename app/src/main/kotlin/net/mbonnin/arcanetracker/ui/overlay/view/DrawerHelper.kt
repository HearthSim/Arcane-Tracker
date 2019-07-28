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

class DrawerHelper(private val view: View, private val handles: HandlesView, private val edge: Edge): ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener  {
    private var mX: Int = 0

    private var mHandlesMovement: Int = 0

    private var direction = 1
    private var velocityRefX: Float = 0f
    private var velocityLastX: Float = 0f
    private var velocityRefTime: Long = 0

    private val mAnimator: ValueAnimator= ValueAnimator()
    private val mHandler = Handler()
    private var mRefY: Float = 0f
    private var mRefX: Float = 0f
    private val mTouchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
    private var mDownY: Float = 0f
    private var mDownX: Float = 0f
    private var viewWidth = 0
    private val mPadding = Utils.dpToPx(5)
    private var buttonWidth = 0

    private val mViewManager = ViewManager.get()
    private val viewParams = ViewManager.Params()
    private val handlesParams = ViewManager.Params()

    private val mHideViewRunnable = {
        viewParams.w = 0
        mViewManager.updateView(view, viewParams)
    }

    private val mHandlesViewTouchListener = View.OnTouchListener { _, ev ->
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownX = ev.getRawX()
            mDownY = ev.getRawY()
            mRefX = handlesParams.x.toFloat()
            mRefY = handlesParams.y.toFloat()
            mHandlesMovement = 0

        } else if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mHandlesMovement == 0) {
                if (Math.abs(ev.getRawX() - mDownX) > mTouchSlop) {
                    prepareAnimation()

                    velocityRefX = ev.getRawX()
                    velocityLastX = ev.getRawX()
                    velocityRefTime = System.nanoTime()

                    mHandlesMovement = HANDLES_MOVEMENT_X
                } else if (Math.abs(ev.getRawY() - mDownY) > mTouchSlop) {
                    mHandlesMovement = HANDLES_MOVEMENT_Y
                }
            }

            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                if ((ev.getRawX() - velocityLastX) * direction > 0) {
                    velocityLastX = ev.getRawX()
                } else {
                    direction = -direction
                    velocityRefX = ev.getRawX()
                    velocityLastX = ev.getRawX()
                    velocityRefTime = System.nanoTime()
                }
                var newX = (mRefX + ev.getRawX() - mDownX).toInt()
                if (newX > viewWidth) {
                    newX = viewWidth
                } else if (newX < 0) {
                    newX = 0
                }
                setX(newX)
            } else if (mHandlesMovement == HANDLES_MOVEMENT_Y) {
                handlesParams.y = (mRefY + ev.getRawY() - mDownY).toInt()
                mViewManager.updateView(handles, handlesParams)
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_CANCEL || ev.getActionMasked() == MotionEvent.ACTION_UP) {
            if (mHandlesMovement == HANDLES_MOVEMENT_X) {
                var velocity = 0f
                val timeDiff = System.nanoTime() - velocityRefTime
                if (timeDiff > 0) {
                    velocity = 1000f * 1000f * (ev.getRawX() - velocityRefX) / timeDiff
                }
                Timber.w("velocity: %f", velocity)
                if (mX < viewWidth) {
                    if (velocity <= 0) {
                        animateXTo(0, velocity)
                    } else if (velocity > 0) {
                        animateXTo(viewWidth, velocity)
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

        handlesParams.x = mPadding
        handlesParams.y = ViewManager.get().height - handlesParams.h - Utils.dpToPx(50)

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
        mViewManager.updateView(view, viewParams)
    }

    private fun animateXTo(targetX: Int, pixelPerMillisecond: Float) {
        var pxPerMs = pixelPerMillisecond
        pxPerMs = Math.abs(pxPerMs)
        if (pxPerMs < 0.6) {
            pxPerMs = 0.6f
        }
        mAnimator.cancel()

        mAnimator.interpolator = LinearInterpolator()
        if (pxPerMs > 0) {
            mAnimator.duration = (Math.abs(mX - targetX) / pxPerMs).toLong()
        } else {
            mAnimator.duration = 300
        }

        prepareAnimation()
        mAnimator.setIntValues(mX, targetX)
        mAnimator.start()
    }

    private fun animateXTo(targetX: Int) {
        mAnimator.cancel()
        mAnimator.interpolator = AccelerateDecelerateInterpolator()
        mAnimator.duration = 300

        prepareAnimation()
        mAnimator.setIntValues(mX, targetX)
        mAnimator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        setX(animation.animatedValue as Int)
    }

    private fun setX(x: Int) {
        Timber.w("setX: %d", mX);
        mX = x
        view.translationX = (-viewWidth + mX).toFloat()
        handlesParams.x = mX + mPadding
        mViewManager.updateView(handles, handlesParams)
        Onboarding.updateTranslation()
    }

    override fun onAnimationStart(animation: Animator) {

    }

    override fun onAnimationEnd(animation: Animator) {
        //Timber.w("onAnimationEnd: %d", mX);
        if (mX == 0) {
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
        mAnimator.cancel()
        viewWidth = width
        viewParams.w = width
        mViewManager.updateView(view, viewParams)
        setX(viewWidth)
    }

    fun setViewHeight(height: Int) {
        viewParams.h = height
        mViewManager.updateView(view, viewParams)
    }

    fun isOpen(): Boolean {
        return mX == viewWidth
    }

    fun open() {
        animateXTo(viewWidth)
    }

    fun close() {
        animateXTo(0)
    }

    fun notifyHandlesChanged() {
        setButtonWidth(buttonWidth)

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