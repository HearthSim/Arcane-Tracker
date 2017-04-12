package net.mbonnin.arcanetracker;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by martin on 10/21/16.
 */
public class CardsAdapter extends RecyclerView.Adapter {
    private final CardDb cardDb;
    private int mClassIndex;
    private ArrayList<Card> mCardList = new ArrayList<>();
    private Listener mListener;
    private String mSearchQuery;
    private int mCost = -1;
    private ArrayList<String> mDisabledCards = new ArrayList<>();

    public void setCost(int cost) {
        mCost = cost;
        filter();
    }

    public CardsAdapter(CardDb cardDb) {
        this.cardDb = cardDb;
    }

    public void setDisabledCards(ArrayList<String> list) {
        mDisabledCards = list;
        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        void onClick(Card card);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, null);

        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(view) {
        };

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        ((AspectRatioImageView)imageView).setAspectRatio(1.51f);

        view.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (mDisabledCards.contains(mCardList.get(holder.getAdapterPosition()).id)) {
                    return false;
                }
                imageView.setColorFilter(Color.argb(120, 255, 255, 255), PorterDuff.Mode.SRC_OVER);
                return true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
                imageView.clearColorFilter();

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    int position = holder.getAdapterPosition();
                    // not really sure how we could go outside this condition but it happens...
                    if (position < mCardList.size()) {
                        mListener.onClick(mCardList.get(position));
                    }
                }
            }
            return false;
        });
        return holder;
    }

    public void setClass(int classIndex) {
        mClassIndex = classIndex;

        filter();
    }

    public void setSearchQuery(String searchQuery) {
        if (searchQuery.equals("")) {
            mSearchQuery = null;
        } else {
            mSearchQuery = searchQuery.toLowerCase();
        }
        filter();

    }

    private void filter() {
        mCardList.clear();
        List<Card> allCards = cardDb.getCards();

        String playerClass = Card.classIndexToPlayerClass(mClassIndex);
        for (Card card : allCards) {
            if (card.collectible == null || !card.collectible) {
                continue;
            }

            if (card.cost == null) {
                continue;
            }

            if (mCost != -1) {
                if (mCost == 7) {
                    if (card.cost < 7) {
                        continue;
                    }
                } else if (card.cost != mCost) {
                    continue;
                }
            }

            /*if (card.type == null
                    || (!card.type.equals("SPELL") && !card.type.equals("MINION") && !card.type.equals("WEAPON"))) {
                continue;
            }*/
            if (card.playerClass != null && playerClass == null
                    || (card.playerClass == null && playerClass != null)
                    || (playerClass != null && !playerClass.equals(card.playerClass))) {
                continue;
            }

            if (mSearchQuery != null) {
                boolean found = false;
                if (card.text != null && card.text.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found && card.name != null && card.name.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found && card.race != null && card.race.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found) {
                    continue;
                }
            }

            mCardList.add(card);
        }

        Collections.sort(mCardList, (a, b) -> a.cost - b.cost);

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = (ImageView) holder.itemView.findViewById(R.id.imageView);
        TextView textView = (TextView) holder.itemView.findViewById(R.id.textView);

        Card card = mCardList.get(position);

        if (mDisabledCards.contains(card.id)) {
            imageView.setColorFilter(Color.argb(180, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
        } else {
            imageView.clearColorFilter();
        }

        String url = "card://" + card.id;
        Timber.d("fetching " + url);
        int placeHolderRes;
        if (card.rarity.equals(Card.RARITY_LEGENDARY)) {
            placeHolderRes = R.raw.placeholder_legendary;
        } else if (card.type.equals(Card.TYPE_SPELL)) {
            placeHolderRes = R.raw.placeholder_spell;
        } else {
            placeHolderRes = R.raw.placeholder_minion;
        }

        textView.setText(card.name);
        Picasso.with(imageView.getContext())
                .load(url)
                .placeholder(placeHolderRes)
                .into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        textView.setText("");
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }
}
