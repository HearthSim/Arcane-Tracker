package net.mbonnin.arcanetracker;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by martin on 10/21/16.
 */
public class EditableDeckAdapter extends DeckAdapter {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = super.onCreateViewHolder(parent, viewType);

        if (viewType == TYPE_DECK_ENTRY) {
          holder.itemView.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    holder.itemView.findViewById(R.id.overlay).setBackgroundColor(Color.argb(150, 255, 255, 255));
                    return true;
                } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
                    holder.itemView.findViewById(R.id.overlay).setBackgroundColor(Color.TRANSPARENT);
                    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                        DeckEntry entry = (DeckEntry) list.get(holder.getAdapterPosition());
                        mDeck.addCard(entry.card.id, -1);
                    }
                }
                return true;
            });
        }
        return holder;
    }

    public ArrayList<String> getDisabledCards() {
        ArrayList<String> list = new ArrayList<>();
        for (DeckEntry entry: entryList) {
            if (entry.inDeck == 2) {
                list.add(entry.card.id);
            } else if (entry.inDeck == 1 && Card.RARITY_LEGENDARY.equals(entry.card.rarity)) {
                list.add(entry.card.id);
            }
        }

        return list;
    }
}
