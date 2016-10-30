package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.trackobot.Result;
import net.mbonnin.arcanetracker.trackobot.ResultData;
import net.mbonnin.arcanetracker.trackobot.Trackobot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 10/27/16.
 */

public class GameState {

    public static final int MODE_PLAY = 0;
    public static final int MODE_ARENA = 1;

    /**
     * tavern brawl/adventure
     */
    public static final int MODE_OTHER = 2;

    private static int sMode;
    private static boolean sWithCoin;
    private static String sPlayerHeroId;
    private static String sOpponentHeroId;

    public static class CardEvent {
        public String cardId;
        public String zone;
        public boolean isOpponent;
        public boolean isShown;
    }

    public static void onCard(CardEvent event) {
        Timber.w("onCard %s [zone=%s][isOpponent=%b][isShown=%b]", event.cardId, event.zone, event.isOpponent, event.isShown);
        if (!event.isOpponent) {
            if (event.isShown && !"DECK".equals(event.zone)
                    || !event.isShown && !"HAND".equals(event.zone)) {
                return;
            }
            Deck deck1 = DeckList.getPlayerGameDeck();
            Deck deck2 = ArcaneView.getPlayerAnchorView().getDeck();

            deck1.addCard(event.cardId, event.isShown ? 1 : -1);
            if (Settings.get(Settings.AUTO_ADD_CARDS, true)) {
                if (deck2.getCountFor(event.cardId) < deck1.getCountFor(event.cardId)) {
                    deck2.addCard(event.cardId, 1);
                    DeckList.save();
                }
            }
        } else {
            if (!"HAND".equals(event.zone) && !"SECRET".equals(event.zone)) {
                return;
            }
            DeckList.getOpponentGameDeck().addCard(event.cardId, 1);
        }
    }

    public static void onGameEnd(boolean victory) {
        Timber.w("onGameEnd %s", victory ? "victory" : "lost");

        if (sMode == MODE_ARENA || sMode == MODE_PLAY
                && Trackobot.get().getUser() != null) {
            ResultData resultData = new ResultData();
            resultData.result = new Result();
            resultData.result.coin = sWithCoin;
            resultData.result.win = victory;
            resultData.result.mode = sMode == MODE_PLAY ? "ranked" : "arena";
            resultData.result.hero = Trackobot.getHero(sPlayerHeroId);
            resultData.result.opponent = Trackobot.getHero(sOpponentHeroId);

            Trackobot.get().sendResult(resultData);
        }
    }

    public static void onGameStart(String playerHeroId, String opponentHeroId, ArrayList<CardEvent> initialCards) {
        Timber.w("onGameStart [heroId=%s][opponentHeroId=%s]", playerHeroId, opponentHeroId);

        sWithCoin = initialCards.size() == 4;
        sPlayerHeroId = playerHeroId;
        sOpponentHeroId = opponentHeroId;

        DeckList.getPlayerGameDeck().clear();
        DeckList.getOpponentGameDeck().clear();
        DeckList.getOpponentGameDeck().classIndex = Card.heroIdToClassIndex(opponentHeroId);

        int classIndex = Card.heroIdToClassIndex(playerHeroId);
        if (Settings.get(Settings.AUTO_SELECT_DECK, true) && classIndex >= 0) {
            if (sMode == MODE_ARENA) {
                Deck deck = DeckList.getArenaDeck();
                ArcaneView.getPlayerAnchorView().setDeck(deck);
            } else {
                activateBestDeck(classIndex, initialCards);
            }
        }

        ArcaneView.getOpponentAnchorView().setDeck(DeckList.getOpponentGameDeck());

        for (CardEvent event: initialCards) {
            // send the initial cards
            onCard(event);
        }
    }

    private static void activateBestDeck(int classIndex, ArrayList<CardEvent> initialCards) {
        Deck deck = ArcaneView.getPlayerAnchorView().getDeck();
        if (deckMatches(deck, classIndex, initialCards)) {
            // the current deck works fine
            return;
        }

        // sort the deck list by descending number of cards. We'll try to get the one with the most cards.
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < DeckList.get().size(); i++) {
            index.add(i);
        }

        Collections.sort(index, (a,b) -> DeckList.get().get(b).getCardCount() - DeckList.get().get(a).getCardCount());

        for (Integer i: index) {
            Deck candidateDeck = DeckList.get().get(i);
            if (deckMatches(candidateDeck, classIndex, initialCards)) {
                ArcaneView.getPlayerAnchorView().setDeck(candidateDeck);
                return;
            }
        }

        /**
         * No good candidate, create a new deck
         */
        Deck deck2 = DeckList.createDeck(classIndex);
        ArcaneView.getPlayerAnchorView().setDeck(deck2);
    }

    private static boolean deckMatches(Deck deck, int classIndex, ArrayList<CardEvent> initialCards) {
        if (deck.classIndex != classIndex) {
            return false;
        }

        int cardCount = 0;
        HashMap<String, Integer> copy = new HashMap<>(deck.cards);
        HashMap<String, Integer> newCards = new HashMap<>();
        for (CardEvent event: initialCards) {
            if (copy.containsKey(event.cardId)) {
                Integer a = copy.get(event.cardId);
                a--;
                if (a < 0) {
                    return false;
                }
                copy.put(event.cardId, a);
                cardCount++;
            } else {
                if (!newCards.containsKey(event.cardId)) {
                    newCards.put(event.cardId, 1);
                } else {
                    newCards.put(event.cardId, newCards.get(event.cardId) + 1);
                }
            }
        }

        copy.putAll(newCards);
        for (String key: copy.keySet()) {
            cardCount += copy.get(key);
        }

        if (cardCount > 30) {
            return false;
        }

        return true;
    }

    public static String modeToString(int mode) {
        switch (mode) {
            case MODE_ARENA:
                return "ARENA";
            case MODE_PLAY:
                return "PLAY";
            case MODE_OTHER:
                return "OTHER";
        }

        return "?";
    }

    public static void onModeChanged(int newMode) {
        Timber.w("onModeChanged newMode=%s", modeToString(newMode));
        sMode = newMode;

    }
}
