package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.FlatGame;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.Player;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.arcanetracker.trackobot.Result;
import net.mbonnin.arcanetracker.trackobot.ResultData;
import net.mbonnin.arcanetracker.trackobot.Trackobot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */
public class ParserListenerPower implements GameLogic.Listener {

    @Override
    public void onGameStarted(GameLogic gameLogic) {
        Timber.w("onGameStarted");

        Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();
        if (Settings.get(Settings.AUTO_SELECT_DECK, true)) {
            if (ParserListenerLoadingScreen.get().getMode() == LoadingScreenParser.MODE_ARENA) {
                deck = DeckList.getArenaDeck();
            } else {
                int classIndex = gameLogic.getPlayer().classIndex();

                deck = activateBestDeck(classIndex, Utils.filterCollectibleCards(gameLogic.getPlayer().zone(Entity.ZONE_HAND)));
            }
        }

        MainViewCompanion.getPlayerCompanion().setDeck(deck, gameLogic.getPlayer());

        DeckList.getOpponentDeck().clear();
        DeckList.getOpponentDeck().classIndex = gameLogic.getOpponent().classIndex();
        MainViewCompanion.getOpponentCompanion().setDeck(DeckList.getOpponentDeck(), gameLogic.getOpponent());
    }

    private static Deck activateBestDeck(int classIndex, HashMap<String, Integer> initialCards) {
        Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();
        if (deckScore(deck, classIndex, initialCards) != -1) {
            // the current deck works fine
            return deck;
        }

        // sort the deck list by descending number of cards. We'll try to get the one with the most cards.
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < DeckList.get().size(); i++) {
            index.add(i);
        }

        Collections.sort(index, (a, b) -> DeckList.get().get(b).getCardCount() - DeckList.get().get(a).getCardCount());

        int maxScore = -1;
        Deck bestDeck = null;

        for (Integer i: index) {
            Deck candidateDeck = DeckList.get().get(i);

            int score = deckScore(candidateDeck, classIndex, initialCards);

            Timber.i("Deck selection " + candidateDeck.name + " has score " + score);
            if (score > maxScore) {
                bestDeck = candidateDeck;
                maxScore = score;
            }
        }

        if (bestDeck == null) {
            /**
             * No good candidate, create a new deck
             */
            bestDeck = DeckList.createDeck(classIndex);
        }

        return bestDeck;
    }

    /**
     *
     */
    private static int deckScore(Deck deck, int classIndex, HashMap<String, Integer> mulliganCards) {
        if (deck.classIndex != classIndex) {
            return -1;
        }

        int matchedCards = 0;
        int newCards = 0;

        /**
         * copy the cards
         */
        HashMap<String, Integer> deckCards = new HashMap<>(deck.cards);

        /**
         * iterate through the mulligan cards.
         *
         * count the one that match the original deck and remove them from the original deck
         *
         * if a card is not in the original deck, increase newCards. At the end, if the total of cards is > 30, the deck is not viable
         */
        for (String cardId: mulliganCards.keySet()) {
            int inDeck = Utils.cardMapGet(deckCards, cardId);
            int inMulligan = Utils.cardMapGet(mulliganCards, cardId);

            int a = Math.min(inDeck, inMulligan);

            Utils.cardMapAdd(deckCards, cardId, -a);
            newCards += inMulligan - a;
            matchedCards += a;
        }

        if (Utils.cardMapTotal(deckCards) + matchedCards + newCards > Deck.MAX_CARDS) {
            return -1;
        }

        return matchedCards;
    }
    @Override
    public void onGameEnded(GameLogic gameLogic, boolean victory) {
        Timber.w("onGameEnd %s", victory ? "victory" : "lost");
        Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();
        if (victory) {
            deck.wins++;
        } else {
            deck.losses++;
        }
        MainViewCompanion.getPlayerCompanion().setDeck(deck);

        DeckList.save();

        int mode = ParserListenerLoadingScreen.get().getMode();
        if (mode == LoadingScreenParser.MODE_ARENA || mode == LoadingScreenParser.MODE_PLAY
                && Trackobot.get().getUser() != null) {
            ResultData resultData = new ResultData();
            resultData.result = new Result();
            resultData.result.coin = gameLogic.getPlayer().hasCoin;
            resultData.result.win = victory;
            resultData.result.mode = mode == LoadingScreenParser.MODE_PLAY ? "ranked" : "arena";
            resultData.result.hero = Trackobot.getHero(gameLogic.getPlayer().classIndex());
            resultData.result.opponent = Trackobot.getHero(gameLogic.getPlayer().classIndex());

            Trackobot.get().sendResult(resultData);
        }
    }
}
