package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by martin on 10/21/16.
 */

public class ManaSelectionView extends LinearLayout {
    int selectedIndex = -1;
    Listener mListener;

    interface Listener {
        void onClick(int index);
    }
    public ManaSelectionView(Context context) {
        super(context);
        init();
    }

    public ManaSelectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }
    public void init() {
        for (int i = 0; i <=7; i++) {
            TextView textView = new TextView(getContext());
            String text = "" + i;
            if (i == 7) {
                text += "+";
            }
            textView.setText(text);
            int w;
            w = Utils.dpToPx(textView.getContext(), 24);

            LinearLayout.LayoutParams layoutParams = new LayoutParams(w, w);

            int finalI = i;
            textView.setOnClickListener(v -> {
                if (selectedIndex == finalI) {
                    selectedIndex = -1;
                } else {
                    selectedIndex = finalI;
                }
                updateViews();

                mListener.onClick(selectedIndex);
            });

            textView.setTextColor(Color.BLACK);
            textView.setGravity(Gravity.CENTER);
            addView(textView, layoutParams);
        }
    }

    private void updateViews() {
        for (int i = 0; i <=7; i++) {
            if (i == selectedIndex) {
                getChildAt(i).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            } else {
                getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
