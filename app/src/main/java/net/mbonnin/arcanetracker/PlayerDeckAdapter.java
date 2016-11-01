package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PlayerDeckAdapter extends DeckAdapter {

    Deck.Listener mListener = () -> {
        populateList();
    };

    public PlayerDeckAdapter() {
        DeckList.getPlayerGameDeck().registerListener(mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object o = list.get(position);
        if (o instanceof DeckEntry) {
            DeckEntry entry = (DeckEntry) o;

            Integer inGame = DeckList.getPlayerGameDeck().cards.get(entry.card.id);
            if (inGame == null) {
                inGame = 0;
            }
            ((BarViewHolder) holder).bind(entry.card, entry.inDeck - inGame);
        } else {
            super.onBindViewHolder(holder, position);
        }
    }



    @Override
    protected void populateList() {
        super.populateList();
        if (mDeck.getCardCount() < 30) {
            list.add(String.format("%d unknown cards", 30 - mDeck.getCardCount()));
        }
    }
}


