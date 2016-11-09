package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.parser.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class DeckAdapter extends RecyclerView.Adapter implements Deck.Listener {
    static final int TYPE_DECK_ENTRY = 0;
    static final int TYPE_STRING = 1;

    ArrayList<Object> list = new ArrayList<>();
    protected Deck mDeck;

    ArrayList<BarItem> entryList = new ArrayList<>();

    @Override
    public void onDeckChanged() {
        entryList.clear();
        for (Map.Entry<String, Integer> entry: mDeck.cards.entrySet()) {
            BarItem deckEntry = new BarItem();
            deckEntry.card = ArcaneTrackerApplication.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            entryList.add(deckEntry);
        }

        for (BarItem e:entryList) {
            if (e.card.cost == null) {
                Timber.e(new Exception("bad card " + e.card.id));
                e.card.cost = 0;
            }
        }
        Collections.sort(entryList, (a,b) -> {
            int ret = a.card.cost - b.card.cost;
            if (ret == 0) {
                ret = a.card.name.compareTo(b.card.name);
            }
            return ret;
        });

        populateList();
    }

    protected void populateList() {
        list.clear();
        list.addAll(entryList);
        notifyDataSetChanged();
    }

    public void setList(ArrayList list) {
        this.list = list;
        notifyDataSetChanged();
    }

    static class BarViewHolder extends RecyclerView.ViewHolder {
        ImageView background;
        TextView cost;
        TextView name;
        TextView count;
        View overlay;

        public BarViewHolder(View itemView) {
            super(itemView);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30));
            itemView.setLayoutParams(params);
            background = ((ImageView)(itemView.findViewById(R.id.background)));
            cost = ((TextView)itemView.findViewById(R.id.cost));
            name = ((TextView)itemView.findViewById(R.id.name));
            count = ((TextView)itemView.findViewById(R.id.count));
            overlay = itemView.findViewById(R.id.overlay);
        }

        public void bind(Card card, int c) {
            background.setImageDrawable(Utils.getDrawableForName(card.id));
            cost.setText(card.cost + "");
            name.setText(card.name);
            if (c > 0) {
                overlay.setBackgroundColor(Color.TRANSPARENT);
                count.setVisibility(View.VISIBLE);
                if (Card.RARITY_LEGENDARY.equals(card.rarity)) {
                    count.setText("\u2605");
                } else {
                    count.setText(c + "");
                }
            } else {
                overlay.setBackgroundColor(Color.argb(150, 0, 0, 0));
                count.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void setDeck(Deck deck) {
        mDeck = deck;
        mDeck.registerListener(this);

        onDeckChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object o = list.get(position);
        if (o instanceof BarItem) {
            return TYPE_DECK_ENTRY;
        } else if (o instanceof String) {
            return TYPE_STRING;
        }

        Timber.e("unsupported type");
        return -1;
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        switch (viewType) {
            case TYPE_DECK_ENTRY: {
                ViewGroup barTemplate = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.bar_template, null);
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bar_card, null);

                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                barTemplate.addView(view, 0, params);

                return new BarViewHolder(barTemplate);
            }
            case TYPE_STRING: {
                ViewGroup barTemplate = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.bar_template, null);
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bar_text, null);
                barTemplate.addView(view, 0);

                RecyclerView.LayoutParams params2 = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(30));
                barTemplate.setLayoutParams(params2);
                return new RecyclerView.ViewHolder(barTemplate) {};
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object o = list.get(position);
        if (o instanceof BarItem) {
            BarItem entry = (BarItem) o;
            ((BarViewHolder)holder).bind(entry.card, entry.count);
        } else if (o instanceof String) {
            ViewGroup barTemplate = (ViewGroup) holder.itemView;
            ((TextView)barTemplate.getChildAt(0)).setText((String) o);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
