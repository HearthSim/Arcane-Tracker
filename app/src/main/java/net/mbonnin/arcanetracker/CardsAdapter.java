package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by martin on 10/21/16.
 */
public class CardsAdapter extends RecyclerView.Adapter {
    private String mClass;
    private ArrayList<Card> mCardList = new ArrayList<>();
    private Listener mListener;
    private String mSearchQuery;
    private int mCost = -1;
    private ArrayList<String> mDisabledCards = new ArrayList<>();

    public void setCost(int cost) {
        mCost = cost;
        filter();
    }

    public CardsAdapter() {
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
            int position = holder.getAdapterPosition();
            /**
             * the NO_POSITION case could happen if you click very fast.
             * not sure about the other case..., maybe it's not needed anymore
             */
            if (position == RecyclerView.NO_POSITION
                    || position >= mCardList.size()) {
                return false;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (mDisabledCards.contains(mCardList.get(position).id)) {
                    return false;
                }
                imageView.setColorFilter(Color.argb(120, 255, 255, 255), PorterDuff.Mode.SRC_OVER);
                return true;
            } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
                imageView.clearColorFilter();

                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mListener.onClick(mCardList.get(position));
                }
            }
            return false;
        });
        return holder;
    }

    public void setClass(String clazz) {
        mClass = clazz;

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
        ArrayList<Card> allCards = CardDb.getCards();

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

            if (card.playerClass == null) {
                /**
                 * is that possible ?
                 */
                continue;
            }

            if (!mClass.equals(card.playerClass)) {
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

        Collections.sort(mCardList, (a, b) -> {
            int r = a.cost - b.cost;
            if (r != 0) {
                return r;
            }
            return a.name.compareTo(b.name);
        });

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

        Context context = textView.getContext();

        textView.setText(card.name);

        Picasso.with(context).load(Utils.getCardUrl(card.id)).into(imageView, new Callback() {
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
