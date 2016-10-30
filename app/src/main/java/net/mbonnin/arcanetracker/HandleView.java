package net.mbonnin.arcanetracker;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.esotericsoftware.kryo.util.Util;

/**
 * Created by martin on 10/28/16.
 */

public class HandleView extends ImageView implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private final int mTouchSlop;
    private final ValueAnimator mValueAnimator;
    private final int mPadding;
    private final Paint mBlurPaint;
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

    public HandleView(Context context) {
        super(context);

        // for blur
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        int p = mPadding = Utils.dpToPx(3);
        setPadding(p, p, p, p);

        mViewManager = ViewManager.get();

        mParams = new ViewManager.Params();
        mParams.x = 0;
        mParams.y = 0;


        ViewConfiguration vc = ViewConfiguration.get(getContext());
        mTouchSlop = vc.getScaledTouchSlop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(Utils.dpToPx(5));
        }

        mValueAnimator = new ValueAnimator();
        mValueAnimator.addListener(this);
        mValueAnimator.addUpdateListener(this);
        mValueAnimator.setInterpolator(new AccelerateInterpolator());

        mBlurPaint = new Paint();
        mBlurPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
        mBlurPaint.setColor(Color.argb(150, 0, 0, 0));

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
        canvas.drawCircle(mPadding/2 + canvas.getHeight()/2, mPadding/2 + canvas.getWidth()/2, canvas.getWidth()/2 - mPadding, mBlurPaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLayoutX = mParams.x;
            mLayoutY = mParams.y;
            mDownX = event.getRawX();
            mDownY = event.getRawY();
            hasMoved = false;
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
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && !hasMoved) {
            if (Math.hypot(mLastX - mOriginX, mLastY - mOriginY) < mTouchSlop) {
                performClick();
                return true;
            } else {
                mValueAnimator.cancel();
                mValueAnimator.setFloatValues(0, 1.0f);
                mValueAnimator.start();
            }
        }

        return false;
    }

    public void setSize(int size) {
        mSize = size;
        mParams.w = mSize;
        mParams.h = mSize;
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
