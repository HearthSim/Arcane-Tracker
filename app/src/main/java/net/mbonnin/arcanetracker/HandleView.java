package net.mbonnin.arcanetracker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;

/**
 * Created by martin on 10/28/16.
 */

public class HandleView extends View implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private int mTouchSlop;
    private ValueAnimator mValueAnimator;
    private int mShadowSize;
    private Paint mBlurPaint;
    private ViewManager mViewManager;

    private ViewManager.Params mParams;
    private int mLayoutX, mLayoutY;
    private float mDownX, mDownY;
    private boolean hasMoved;

    private int mSize;
    private int mLastY;
    private int mLastX;
    private int mOriginX;
    private int mOriginY;
    private int mRadius;
    private Paint mColorPaint;
    private Paint mOverlayPaint;
    private boolean mPressed;
    private Drawable mDrawable;

    public HandleView(Context context) {
        super(context);
    }

    public HandleView(Context context, AttributeSet attr) {
        super(context, attr);
        init(null, Color.RED);
    }

    public void init(Drawable drawable, int color) {
        if (isInEditMode()) {
            mSize = 100;
            mShadowSize = 6;
        } else {
            mSize = Utils.dpToPx(50);
            mShadowSize = Utils.dpToPx(3);
            mViewManager = ViewManager.get();
        }

        // for blur
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        mRadius = (mSize - mShadowSize) / 2;

        mParams = new ViewManager.Params();
        mParams.x = 0;
        mParams.y = 0;

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        mValueAnimator = new ValueAnimator();
        mValueAnimator.addListener(this);
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.setInterpolator(new AccelerateInterpolator());

        mBlurPaint = new Paint();
        mBlurPaint.setAntiAlias(true);
        mBlurPaint.setMaskFilter(new BlurMaskFilter(mShadowSize, BlurMaskFilter.Blur.NORMAL));
        mBlurPaint.setColor(Color.argb(150, 0, 0, 0));

        mColorPaint = new Paint();
        mColorPaint.setAntiAlias(true);
        mColorPaint.setColor(color);

        mOverlayPaint = new Paint();
        mOverlayPaint.setAntiAlias(true);
        mOverlayPaint.setColor(Color.argb(128, 255, 255, 255));

        mDrawable = drawable;
    }

    public void setOrigin(int x, int y) {
        mOriginX = x;
        mOriginY = y;
    }

    public void show(boolean show) {
        if (show) {
            mParams.x = mOriginX;
            mParams.y = mOriginY;

            mLastX = mParams.x;
            mLastY = mParams.y;
            mViewManager.addView(this, mParams);
        } else {
            mViewManager.removeView(this);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mPressed) {
            canvas.drawCircle(mRadius + mShadowSize / 2, mRadius + mShadowSize / 2, mRadius, mBlurPaint);
            canvas.drawCircle(mRadius, mRadius, mRadius, mColorPaint);
            if (mDrawable != null) {
                mDrawable.setBounds(0, 0, mSize - mShadowSize, mSize - mShadowSize);
                mDrawable.draw(canvas);
            }
        } else {
            canvas.drawCircle(mRadius + mShadowSize / 2, mRadius + mShadowSize / 2, mRadius, mColorPaint);
            canvas.drawCircle(mRadius + mShadowSize / 2, mRadius + mShadowSize / 2, mRadius, mOverlayPaint);
            if (mDrawable != null) {
                mDrawable.setBounds(mShadowSize / 2, mShadowSize / 2, mSize - mShadowSize / 2, mSize - mShadowSize / 2);
                mDrawable.draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLayoutX = mParams.x;
            mLayoutY = mParams.y;
            mDownX = event.getRawX();
            mDownY = event.getRawY();
            hasMoved = false;
            mPressed = true;
            invalidate();
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (!hasMoved) {
                if (Math.hypot(event.getRawX() - mDownX, event.getRawY() - mDownY) > mTouchSlop) {
                    hasMoved = true;
                }
            }
            if (hasMoved) {
                mParams.x = (int) (mLayoutX + (event.getRawX() - mDownX));
                mParams.y = (int) (mLayoutY + (event.getRawY() - mDownY));

                mLastX = mParams.x;
                mLastY = mParams.y;

                mViewManager.updateView(this, mParams);
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            mPressed = false;
            invalidate();
            if (!hasMoved) {
                if (Math.hypot(mLastX - mOriginX, mLastY - mOriginY) < mTouchSlop) {
                    performClick();
                    return true;
                } else {
                    mValueAnimator.cancel();
                    mValueAnimator.setFloatValues(0, 1.0f);
                    mValueAnimator.start();
                }
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            mPressed = false;
            invalidate();
        }
        return false;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mSize, mSize);
    }


    @Override
    public void onAnimationStart(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
        performClick();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float a = (float) animation.getAnimatedValue();
        mParams.x = (int) (mLastX * (1 - a) + a * (mOriginX));
        mParams.y = (int) (mLastY * (1 - a) + a * (mOriginY));

        mViewManager.updateView(this, mParams);
    }
}
