package net.mbonnin.arcanetracker.ui.overlay.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by martin on 11/3/16.
 */

public class AspectRatioImageView extends AppCompatImageView {
    private float mAspect;

    public AspectRatioImageView(Context context) {
        super(context);
    }

    public AspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAspectRatio(float aspect) {
        mAspect = aspect;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int h = (int) (getMeasuredWidth() * mAspect);
        setMeasuredDimension(getMeasuredWidth(), h);
    }
}
