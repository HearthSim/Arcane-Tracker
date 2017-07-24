package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.CardDb;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by martin on 11/7/16.
 */
public class Player {
    public Entity entity;
    public String battleTag;
    public boolean isOpponent;
    public boolean hasCoin;

    public Entity hero;
    public Entity heroPower;

    public int classIndex() {
        Card card = CardDb.getCard(hero.CardID);
        return Card.playerClassToClassIndex(card.playerClass);
    }
}
