package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.CardUtil;
import net.mbonnin.hsmodel.Card;

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
        Card card = CardUtil.getCard(hero.CardID);
        return Card.Companion.playerClassToClassIndex(card.playerClass);
    }
}
