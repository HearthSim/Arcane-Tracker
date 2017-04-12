package net.mbonnin.arcanetracker.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 10/17/16.
 */

public class ItemAdapter extends RecyclerView.Adapter implements Deck.Listener {
    static final int TYPE_DECK_ENTRY = 0;
    static final int TYPE_STRING = 1;
    static final int TYPE_HEADER = 2;
    private final CardDb cardDb;

    protected List<Object> list = new ArrayList<>();
    protected Deck mDeck;
    protected List<DeckEntryItem> entryList = new ArrayList<>();

    public ItemAdapter(CardDb cardDb) {
        this.cardDb = cardDb;
    }

    @Override
    public void onDeckChanged() {
        entryList.clear();
        for (Map.Entry<String, Integer> entry: mDeck.cards.entrySet()) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            deckEntry.card = cardDb.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            entryList.add(deckEntry);
        }

        for (DeckEntryItem e:entryList) {
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

    public void setList(List list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setDeck(Deck deck) {
        mDeck = deck;
        mDeck.setListener(this);

        onDeckChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object o = list.get(position);
        if (o instanceof DeckEntryItem) {
            return TYPE_DECK_ENTRY;
        } else if (o instanceof String) {
            return TYPE_STRING;
        } else if (o instanceof HeaderItem) {
            return TYPE_HEADER;
        }

        Timber.e("unsupported type");
        return -1;
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view;
        switch (viewType) {
            case TYPE_DECK_ENTRY: {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bar_card, null);
                break;
            }
            case TYPE_STRING: {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bar_text, null);
                break;
            }
            case TYPE_HEADER: {
                view = LayoutInflater.from(context).inflate(R.layout.bar_header, null);
                break;
            }
            default:
                return null;
        }

        ViewGroup barTemplate = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.bar_template, null);
        RecyclerView.LayoutParams params2 = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.dpToPx(context, 30));
        barTemplate.setLayoutParams(params2);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        barTemplate.addView(view, 0, params);

        switch (viewType) {
            case TYPE_DECK_ENTRY:
                return new DeckEntryHolder(barTemplate);
            default:
                return new RecyclerView.ViewHolder(barTemplate) {};
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object o = list.get(position);
        if (o instanceof DeckEntryItem) {
            DeckEntryItem entry = (DeckEntryItem) o;
            ((DeckEntryHolder)holder).bind(entry);
        } else if (o instanceof String) {
            ViewGroup barTemplate = (ViewGroup) holder.itemView;
            ((TextView)barTemplate.getChildAt(0)).setText((String) o);
        } else if (o instanceof HeaderItem) {
            HeaderItem headerItem = (HeaderItem)o;
            TextView textView = (TextView)holder.itemView;
            String text = headerItem.expanded ?"▼":"▶";
            textView.setText(text + headerItem.title);
            textView.setOnClickListener(v -> {
                headerItem.onClicked.run();
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
