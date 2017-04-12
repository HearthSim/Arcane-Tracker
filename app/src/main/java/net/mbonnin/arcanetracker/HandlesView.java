package net.mbonnin.arcanetracker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import javax.inject.Inject;

import internal.di.view.AndroidViewInjection;

public class HandlesView extends LinearLayout {
    private ViewManager.Params mParams = new ViewManager.Params();
    private OnTouchListener mListener;

    @Inject ViewManager viewManager;

    public HandlesView(Context context) {
        super(context);
        init();
    }

    public HandlesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        AndroidViewInjection.inject(this);
    }

    public void show(boolean show) {
        if (show) {
            viewManager.addView(this, mParams);
        } else {
            viewManager.removeView(this);
        }
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
        viewManager.updateView(this, mParams);
    }
}
