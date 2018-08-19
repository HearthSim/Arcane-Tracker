package net.mbonnin.arcanetracker.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by martin on 11/3/16.
 */

public class AspectRatioImageView extends ImageView {
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
