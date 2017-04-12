package net.mbonnin.arcanetracker.adapter;

import net.mbonnin.arcanetracker.CardDb;
import net.mbonnin.arcanetracker.parser.EntityList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 11/21/16.
 */

public class OpponentController extends Controller {
    private final CardDb cardDb;
    private final ItemAdapter mAdapter;

    private HeaderItem mHandHeader = new HeaderItem();

    public OpponentController(CardDb cardDb, String handHeader, ItemAdapter adapter) {
        this.cardDb = cardDb;
        mAdapter = adapter;
        mHandHeader.title = handHeader;
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
            deckEntry.card = cardDb.getCard(entry.getKey());
            deckEntry.count = entry.getValue();
            knownItems.add(deckEntry);
        }
        list.addAll(knownItems);

        Collections.sort(list, DeckEntryItem.COMPARATOR);

        mAdapter.setList(list);
    }
}
