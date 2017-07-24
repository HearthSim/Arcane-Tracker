package net.mbonnin.arcanetracker.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.ViewGroup;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Created by martin on 10/21/16.
 */
public class EditableItemAdapter extends ItemAdapter {
    private Deck mDeck;

    public void setDeck(Deck deck) {
        mDeck = deck;

        update();
    }

    public void addCard(String cardId) {
        Utils.cardMapAdd(mDeck.cards, cardId, 1);
        update();
    }

    private void update() {
        ArrayList<DeckEntryItem> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry: mDeck.cards.entrySet()) {
            DeckEntryItem item = new DeckEntryItem();
            item.card = CardDb.getCard(entry.getKey());
            item.count = entry.getValue();
            list.add(item);
        }

        Collections.sort(list, (a,b) -> a.card.cost - b.card.cost);

        setList(list);
    }

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
                        int position = holder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            DeckEntryItem entry = (DeckEntryItem) list.get(position);
                            mDeck.addCard(entry.card.id, -1);
                            update();
                        }
                    }
                }
                return true;
            });
        }
        return holder;
    }

    public ArrayList<String> getDisabledCards() {
        ArrayList<String> list = new ArrayList<>();
        if (!mDeck.isArena()) {
            for (Object o: super.list) {
                DeckEntryItem deckEntryItem = (DeckEntryItem)o;
                if (deckEntryItem.count == 2) {
                    list.add(deckEntryItem.card.id);
                } else if (deckEntryItem.count == 1 && Card.RARITY_LEGENDARY.equals(deckEntryItem.card.rarity)) {
                    list.add(deckEntryItem.card.id);
                }
            }
        }

        return list;
    }
}
