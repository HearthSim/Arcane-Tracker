package net.mbonnin.arcanetracker;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by martin on 10/21/16.
 */

public class ViewManager {
    public static ViewManager sViewManager;
    private final int mScreenWidth;
    private final int mScreenHeight;
    private final int mUsableWidth;
    private final int mUsableHeight;
    private WindowManager mWindowManager;
    private ArrayList<View> mViews = new ArrayList<>();

    public int getUsableWidth() {
        return mUsableWidth;
    }

    public int getUsableHeight() {
        return mUsableHeight;
    }

    public void addCenteredView(View view) {
        Params params = new Params();

        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(wMeasureSpec, wMeasureSpec);

        params.x = (mScreenWidth - view.getMeasuredWidth()) / 2;
        params.y = (mScreenHeight - view.getMeasuredHeight()) / 2;
        params.w = view.getMeasuredWidth();
        params.h = view.getMeasuredHeight();

        addModalAndFocusableView(view, params);
    }

    public void removeAllViewsExcept(View view) {
        Iterator<View> it = mViews.iterator();
        while (it.hasNext()) {
            View view2 = it.next();
            if (view != view2) {
                it.remove();
                mWindowManager.removeView(view2);
            }
        }
    }

    public static class Params {
        public int x, y, w, h;
    }

    public ViewManager(Context context) {
        mWindowManager = (WindowManager)context.getSystemService(Activity.WINDOW_SERVICE);
        Point screenSize = new Point();

        mWindowManager.getDefaultDisplay().getSize(screenSize);
        if (screenSize.x > screenSize.y) {
            mUsableWidth = screenSize.x;
            mUsableHeight = screenSize.y;
        } else {
            mUsableHeight = screenSize.x;
            mUsableWidth = screenSize.y;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWindowManager.getDefaultDisplay().getRealSize(screenSize);
            if (screenSize.x > screenSize.y) {
                mScreenWidth = screenSize.x;
                mScreenHeight = screenSize.y;
            } else {
                mScreenHeight = screenSize.x;
                mScreenWidth = screenSize.y;
            }
        } else {
            /**
             * best effort
             */
            mScreenWidth = mUsableWidth;
            mScreenHeight = mUsableHeight;
        }
    }

    public static  ViewManager get() {
        if (sViewManager == null) {
            sViewManager = new ViewManager(ArcaneTrackerApplication.getContext());
        }

        return sViewManager;
    }

    public void addModalAndFocusableView(View view, Params params) {
        addModalViewIternal(view, params, 0);
    }
    public void addModalViewIternal(View view, Params params, int extraFlags) {
        addViewInternal(view, params, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |  WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | extraFlags);
        view.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                removeView(view);
            }
            return false;
        });
    }
    public void addModalView(View view, Params params) {
        addModalViewIternal(view, params, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
    private void addViewInternal(View view, Params params, int flags) {
        if (mViews.contains(view)) {
            return;
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                params.w,
                params.h,
                WindowManager.LayoutParams.TYPE_PHONE,
                flags,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = params.x;
        layoutParams.y = params.y;

        mViews.add(view);
        mWindowManager.addView(view, layoutParams);

    }
    public void addView(View view, Params params) {
        addViewInternal(view, params, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    public void removeView(View view) {
        if (!mViews.contains(view)) {
            return;
        }
        mViews.remove(view);
        mWindowManager.removeView(view);
    }
    public void updateView(View view, Params r) {
        if (!mViews.contains(view)) {
            return;
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) view.getLayoutParams();
        layoutParams.width = r.w;
        layoutParams.height = r.h;
        layoutParams.x = r.x;
        layoutParams.y = r.y;

        mWindowManager.updateViewLayout(view, layoutParams);

    }

    public int getWidth() {
        return mScreenWidth;
    }

    public int getHeight() {
        return mScreenHeight;
    }

    public void removeAllViews() {
        while (!mViews.isEmpty()) {
            View view = mViews.get(0);
            removeView(view);
        }
    }

}
