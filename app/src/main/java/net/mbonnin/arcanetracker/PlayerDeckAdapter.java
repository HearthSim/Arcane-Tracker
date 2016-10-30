package net.mbonnin.arcanetracker;

import android.support.v7.widget.RecyclerView;

public class PlayerDeckAdapter extends DeckAdapter {

    Deck.Listener mListener = () -> {
        notifyDataSetChanged();
    };

    public PlayerDeckAdapter() {
        DeckList.getPlayerGameDeck().registerListener(mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        DeckEntry entry = list.get(position);

        Integer inGame = DeckList.getPlayerGameDeck().cards.get(entry.card.id);
        if (inGame == null) {
            inGame = 0;
        }
        ((BarViewHolder) holder).bind(entry.card, entry.inDeck - inGame);
    }
}


