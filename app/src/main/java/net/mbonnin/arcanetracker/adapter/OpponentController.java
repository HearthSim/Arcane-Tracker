package net.mbonnin.arcanetracker.adapter;

import android.text.TextUtils;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 11/21/16.
 */

public class OpponentController extends Controller {
    private final ItemAdapter mAdapter;

    HeaderItem mHandHeader = new HeaderItem();

    public OpponentController(ItemAdapter adapter) {
        mAdapter = adapter;
        mHandHeader.title = ArcaneTrackerApplication.getContext().getString(R.string.hand);
        mHandHeader.onClicked = () -> {
            mHandHeader.expanded = !mHandHeader.expanded;
            update();
        };
    }

    @Override
    protected void update() {
        if (mDeck == null) {
            return;
        }

        EntityList entities;
        if (mPlayer == null) {
            entities = new EntityList();
        } else {
            entities = mPlayer.entities;
        }

        ArrayList list = new ArrayList();
        /*list.add(mHandHeader);
        if (mHandHeader.expanded) {
            EntityList handEntities = entities.filter(EntityList.IS_IN_HAND);
            ArrayList<DeckEntryItem> handItems = new ArrayList<>();
            for (Entity entity: handEntities) {
                DeckEntryItem deckEntry = new DeckEntryItem();
                if (TextUtils.isEmpty(entity.CardID)) {
                    deckEntry.card = Card.unknown();
                } else {
                    deckEntry.card = CardDb.getCard(entity.CardID);
                }
                deckEntry.count = 1;
                handItems.add(deckEntry);
            }
            list.addAll(handItems);
        }

        list.add(mHandHeader);*/
        EntityList playedEntities = entities.filter(EntityList.IS_FROM_ORIGINAL_DECK)
                .filter(EntityList.IS_NOT_IN_DECK);
        HashMap<String, Integer> map = playedEntities.toCardMap();

        ArrayList<DeckEntryItem> knownItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            DeckEntryItem deckEntry = new DeckEntryItem();
            deckEntry.card = CardDb.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            knownItems.add(deckEntry);
        }
        list.addAll(knownItems);

        Collections.sort(list, DeckEntryItem.COMPARATOR);

        mAdapter.setList(list);
    }
}
