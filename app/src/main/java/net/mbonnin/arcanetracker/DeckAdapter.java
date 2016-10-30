package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class DeckAdapter extends RecyclerView.Adapter implements Deck.Listener {
    ArrayList<DeckEntry> list = new ArrayList<>();
    protected Deck mDeck;

    @Override
    public void onDeckChanged() {
        list.clear();
        for (Map.Entry<String, Integer> entry: mDeck.cards.entrySet()) {
            DeckEntry deckEntry = new DeckEntry();
            deckEntry.card = ArcaneTrackerApplication.getCard(entry.getKey());
            deckEntry.inDeck = entry.getValue();
            list.add(deckEntry);
        }
        sort();
        notifyDataSetChanged();
    }

    public static class DeckEntry {
        public Card card;
        int inDeck;
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

    protected void sort() {
        for (DeckEntry e:list) {
            if (e.card.cost == null) {
                Timber.e(new Exception("bad card " + e.card.id));
                e.card.cost = 0;
            }
        }
        Collections.sort(list, (a,b) -> {
            int ret = a.card.cost - b.card.cost;
            if (ret == 0) {
                ret = a.card.name.compareTo(b.card.name);
            }
            return ret;
        });
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bar, null);
        return new BarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        DeckEntry entry = list.get(position);

        ((BarViewHolder)holder).bind(entry.card, entry.inDeck);
    }



    protected DeckEntry getEntry(String id) {
        for (DeckEntry entry: list) {
            if(entry.card.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
