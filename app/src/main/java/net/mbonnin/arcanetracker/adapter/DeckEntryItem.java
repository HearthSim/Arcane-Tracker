package net.mbonnin.arcanetracker.adapter;

import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.hsmodel.Card;


public class DeckEntryItem {

    public Card card;
    public int count;
    public boolean gift;

    public EntityList entityList = new EntityList();
}
