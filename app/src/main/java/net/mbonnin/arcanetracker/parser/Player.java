package net.mbonnin.arcanetracker.parser;

import android.text.TextUtils;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.Utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by martin on 11/7/16.
 */
public class Player {


    public HashMap<String, Integer> getKnownCollectibleCards() {
        HashMap<String, Integer> knownCards = new HashMap<>();
        for (CardEntity cardEntity : cards) {
            String cardId = cardEntity.CardID;
            if (TextUtils.isEmpty(cardId)) {
                continue;
            }

            if (!Card.isCollectible(cardId)) {
                continue;
            }


            Utils.cardMapAdd(knownCards, cardId, 1);
        }
        return knownCards;
    }

    public int classIndex() {
        Card card = ArcaneTrackerApplication.getCard(hero.CardID);
        return Card.playerClassToClassIndex(card.playerClass);
    }

    public interface Listener {
        void onPlayerStateChanged();
    }

    public PlayerEntity entity;
    public String battleTag;
    public boolean isOpponent;
    public boolean hasCoin;

    public CardEntity hero;
    public CardEntity heroPower;

    public ArrayList<CardEntity> cards = new ArrayList<>();

    public void reset() {
        cards.clear();
    }

    public ArrayList<Listener> listeners = new ArrayList<>();

}
