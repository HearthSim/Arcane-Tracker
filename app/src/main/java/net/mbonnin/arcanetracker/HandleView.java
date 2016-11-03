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

public class HandleView extends View {
    private int mTouchSlop;
    private int mShadowSize;
    private Paint mBlurPaint;
    private ViewManager mViewManager;

    private ViewManager.Params mParams;
    private int mLayoutX, mLayoutY;
    private float mDownX, mDownY;
    private boolean hasMoved;

    private int mLastY;
    private int mLastX;
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
        mShadowSize = Utils.dpToPx(3);
        mViewManager = ViewManager.get();

        // for blur
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        mParams = new ViewManager.Params();
        mParams.x = 0;
        mParams.y = 0;

        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();

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


    public void show(boolean show) {
        if (show) {
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
        int radius = (getMeasuredWidth() - mShadowSize)/2;
        if (!mPressed) {
            canvas.drawCircle(radius + mShadowSize / 2, radius + mShadowSize / 2, radius, mBlurPaint);
            canvas.drawCircle(radius, radius, radius, mColorPaint);
            if (mDrawable != null) {
                mDrawable.setBounds(0, 0, getMeasuredWidth() - mShadowSize, getMeasuredWidth() - mShadowSize);
                mDrawable.draw(canvas);
            }
        } else {
            canvas.drawCircle(radius + mShadowSize / 2, radius + mShadowSize / 2, radius, mColorPaint);
            canvas.drawCircle(radius + mShadowSize / 2, radius + mShadowSize / 2, radius, mOverlayPaint);
            if (mDrawable != null) {
                mDrawable.setBounds(mShadowSize / 2, mShadowSize / 2, getMeasuredWidth() - mShadowSize / 2, getMeasuredWidth() - mShadowSize / 2);
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
                if (Math.hypot(mLastX, mLastY) < mTouchSlop) {
                    performClick();
                    return true;
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
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
    }
}
