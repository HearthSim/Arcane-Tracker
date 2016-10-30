package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * Created by martin on 10/21/16.
 */
public class EditableDeckAdapter extends DeckAdapter {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = super.onCreateViewHolder(parent, viewType);

        holder.itemView.setOnClickListener(v -> {
            DeckEntry entry = list.get(holder.getAdapterPosition());
            mDeck.addCard(entry.card.id, -1);
        });
        return holder;
    }
}
