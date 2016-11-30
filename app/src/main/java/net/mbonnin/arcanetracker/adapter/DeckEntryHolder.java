package net.mbonnin.arcanetracker.adapter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardRenderer;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.ViewManager;

import timber.log.Timber;

import static android.view.View.GONE;

class DeckEntryHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
    private final Handler mHandler;
    ImageView gift;
    ImageView background;
    TextView cost;
    TextView name;
    TextView count;
    View overlay;

    Card card;
    private DetailsView detailsView;

    private Bitmap bitmap;
    private boolean longPress;

    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            longPress = true;
            displayImageViewIfNeeded();
        }

    };
    private float downY;
    private float downX;
    private DeckEntryItem deckEntry;

    private void displayImageViewIfNeeded() {
        if (longPress &&
                ("?".equals(card.id) || bitmap != null)
                && deckEntry != null) {
            if (detailsView != null) {
                Timber.d("too many imageViews");
                return;
            }

            detailsView = new DetailsView(itemView.getContext());

            /**
             * bitmap might be null if the card comes from the Hand
             */
            detailsView.configure(bitmap, deckEntry, (int) (ViewManager.get().getHeight()/1.5f));

            ViewManager.Params params = new ViewManager.Params();

            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            detailsView.measure(measureSpec, measureSpec);
            params.w = detailsView.getMeasuredWidth();
            params.h = detailsView.getMeasuredHeight();

            params.x = (int) (downX + Utils.dpToPx(40));
            params.y = (int) (downY - params.h / 2);
            if (params.y < 0) {
                params.y = 0;
            } else if (params.y + params.h > ViewManager.get().getHeight()) {
                params.y = ViewManager.get().getHeight() - params.h;
            }
            ViewManager.get().addModalView(detailsView, params);
        }

    }
    public DeckEntryHolder(View itemView) {
        super(itemView);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30));
        itemView.setLayoutParams(params);
        background = ((ImageView)(itemView.findViewById(R.id.background)));
        cost = ((TextView)itemView.findViewById(R.id.cost));
        name = ((TextView)itemView.findViewById(R.id.name));
        count = ((TextView)itemView.findViewById(R.id.count));
        overlay = itemView.findViewById(R.id.overlay);
        gift = (ImageView)itemView.findViewById(R.id.gift);

        mHandler = new Handler();
        itemView.setOnTouchListener(this);
    }


    public void bind(DeckEntryItem entry) {
        this.card = entry.card;
        int c = entry.count;

        Picasso.with(itemView.getContext())
                .load("bar://" + card.id)
                .placeholder(R.drawable.hero_10)
                .into(background);

        int costInt = Utils.valueOf(card.cost);
        if (costInt >= 0) {
            cost.setText(costInt + "");
            cost.setVisibility(View.VISIBLE);
        } else {
            cost.setVisibility(View.INVISIBLE);
        }
        name.setText(card.name);
        count.setVisibility(GONE);
        gift.setVisibility(GONE);

        resetImageView();

        deckEntry = entry;
        if (c > 0) {
            overlay.setBackgroundColor(Color.TRANSPARENT);

            if (entry.gift) {
                gift.setVisibility(View.VISIBLE);
            } else if (Card.RARITY_LEGENDARY.equals(card.rarity)) {
                count.setVisibility(View.VISIBLE);
                count.setText("\u2605");
            } else if (c > 1){
                count.setVisibility(View.VISIBLE);
                count.setText(c + "");
            }
        } else {
            overlay.setBackgroundColor(Color.argb(150, 0, 0, 0));
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            if (!"?".equals(card.id)) {
                Picasso.with(v.getContext()).load(Utils.getCardUrl(card.id)).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        DeckEntryHolder.this.bitmap = bitmap;
                        displayImageViewIfNeeded();
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
            }
            mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
            resetImageView();
        }

        return true;
    }

    private void resetImageView() {
        if (detailsView != null) {
            ViewManager.get().removeView(detailsView);
            detailsView = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        longPress = false;
        bitmap = null;
    }
}
