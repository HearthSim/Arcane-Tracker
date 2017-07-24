package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

public class GemView extends View {
    private String mText;

    public GemView(Context context) {
        super(context);
    }

    public void setText(String text) {
        mText = text;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable drawable = getResources().getDrawable(R.drawable.cost_mana);
        drawable.draw(canvas);

        int size = (int) (canvas.getHeight());
        float dx = canvas.getWidth()/2;
        float dy = canvas.getHeight() * 3 /4;


        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setTypeface(Typefaces.belwe());
        paint.setTextSize(size);
        if (mText.length() > 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                paint.setLetterSpacing(-0.15f);
            }
        }

        canvas.drawText(mText, dx, dy, paint);

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(size * 0.03f);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawText(mText, dx, dy, paint);
    }
}
