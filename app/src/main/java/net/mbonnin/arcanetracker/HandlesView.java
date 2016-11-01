package net.mbonnin.arcanetracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

public class HandlesView extends LinearLayout {
    private ViewManager.Params mParams;
    private OnTouchListener mListener;

    public HandlesView(Context context) {
        super(context);
        init();
    }

    public HandlesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mParams = new ViewManager.Params();
    }

    public void show() {
        ViewManager.get().addView(this, mParams);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mListener != null) {
            return mListener.onTouch(this, ev);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mListener != null) {
            return mListener.onTouch(this, ev);
        }
        return false;
    }

    public ViewManager.Params getParams() {
        return mParams;
    }

    public void setListener(OnTouchListener listener) {
        mListener = listener;
    }

    public void update() {
        ViewManager.get().updateView(this, mParams);
    }
}
