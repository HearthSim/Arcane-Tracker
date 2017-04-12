package net.mbonnin.arcanetracker;

import com.google.firebase.analytics.FirebaseAnalytics;

import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.Play;
import net.mbonnin.arcanetracker.parser.PowerParser;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.model.CardPlay;
import net.mbonnin.arcanetracker.trackobot.model.Result;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */
public class ParserListenerPower implements PowerParser.Listener {

    private final ParserListenerLoadingScreen parserListenerLoadingScreen;
    private final MainViewCompanion mainViewCompanion;
    private final DeckListManager deckListManager;
    private final Settings settings;
    private final Trackobot trackobot;
    private final FirebaseAnalytics firebaseAnalytics;
    private Game mLastGame;

    public ParserListenerPower(ParserListenerLoadingScreen parserListenerLoadingScreen, MainViewCompanion mainViewCompanion, DeckListManager deckListManager, Settings settings, Trackobot trackobot, FirebaseAnalytics firebaseAnalytics) {
        this.parserListenerLoadingScreen = parserListenerLoadingScreen;
        this.mainViewCompanion = mainViewCompanion;
        this.deckListManager = deckListManager;
        this.settings = settings;
        this.trackobot = trackobot;
        this.firebaseAnalytics = firebaseAnalytics;
    }

    @Override
    public void onGameStarted(Game game) {
        Timber.w("onGameStarted");

        mLastGame = game;

        Deck deck = mainViewCompanion.getPlayerCompanion().getDeck();
        if (settings.get(Settings.AUTO_SELECT_DECK, true)) {
            if (parserListenerLoadingScreen.getMode() == LoadingScreenParser.MODE_ARENA) {
                deck = deckListManager.getArenaDeck();
            } else {
                int classIndex = game.getPlayer().classIndex();

                /**
                 * we filter the original deck to remove the coin mainly
                 */
                HashMap<String, Integer> map = game.getPlayer().zone(Entity.ZONE_HAND)
                        .filter(EntityList.IS_FROM_ORIGINAL_DECK)
                        .toCardMap();

                deck = activateBestDeck(classIndex, map);
            }
        }

        mainViewCompanion.getPlayerCompanion().setDeck(deck, game.getPlayer());

        deckListManager.getOpponentDeck().clear();
        deckListManager.getOpponentDeck().classIndex = game.getOpponent().classIndex();
        mainViewCompanion.getOpponentCompanion().setDeck(deckListManager.getOpponentDeck(), game.getOpponent());
    }

    private Deck activateBestDeck(int classIndex, Map<String, Integer> initialCards) {
        Deck deck = mainViewCompanion.getPlayerCompanion().getDeck();
        if (deckScore(deck, classIndex, initialCards) != -1) {
            // the current deck works fine
            return deck;
        }

        // sort the deck list by descending number of cards. We'll try to get the one with the most cards.
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < deckListManager.get().size(); i++) {
            index.add(i);
        }

        Collections.sort(index, (a, b) -> deckListManager.get().get(b).getCardCount() - deckListManager.get().get(a).getCardCount());

        int maxScore = -1;
        Deck bestDeck = null;

        for (Integer i : index) {
            Deck candidateDeck = deckListManager.get().get(i);

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
//            bestDeck = DeckListManager.createDeck(classIndex);
        }

        return bestDeck;
    }

    /**
     *
     */
    private int deckScore(Deck deck, int classIndex, Map<String, Integer> mulliganCards) {
        if (deck.classIndex != classIndex) {
            return -1;
        }

        int matchedCards = 0;
        int newCards = 0;

        /**
         * copy the cards
         */
        Map<String, Integer> deckCards = new HashMap<>(deck.cards);

        /**
         * iterate through the mulligan cards.
         *
         * count the one that match the original deck and remove them from the original deck
         *
         * if a card is not in the original deck, increase newCards. At the end, if the total of cards is > 30, the deck is not viable
         */
        for (String cardId : mulliganCards.keySet()) {
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
    public void onGameEnded(Game game, boolean victory) {
        Timber.w("onGameEnd %s", victory ? "victory" : "lost");
        Deck deck = mainViewCompanion.getPlayerCompanion().getDeck();
        if (victory) {
            deck.wins++;
        } else {
            deck.losses++;
        }
        mainViewCompanion.getPlayerCompanion().setDeck(deck);

        deckListManager.save();

        int mode = parserListenerLoadingScreen.getMode();
        if (mode == LoadingScreenParser.MODE_ARENA || mode == LoadingScreenParser.MODE_PLAY
                && trackobot.getUser() != null) {
            ResultData resultData = new ResultData();
            resultData.result = new Result();
            resultData.result.coin = game.getPlayer().hasCoin;
            resultData.result.win = victory;
            resultData.result.mode = mode == LoadingScreenParser.MODE_PLAY ? "ranked" : "arena";
            resultData.result.hero = Trackobot.getHero(game.player.classIndex());
            resultData.result.opponent = Trackobot.getHero(game.opponent.classIndex());
            resultData.result.added = Utils.ISO8601DATEFORMAT.format(new Date());

            ArrayList<CardPlay> history = new ArrayList<>();
            for (Play play: game.plays) {
                CardPlay cardPlay = new CardPlay();
                cardPlay.player = play.isOpponent ? "opponent": "me";
                cardPlay.turn = (play.turn + 1)/2;
                cardPlay.card_id = play.cardId;
                history.add(cardPlay);
            }

            resultData.result.card_history = history;

            trackobot.sendResult(resultData);
        }

        firebaseAnalytics.logEvent("game_ended", null);
    }

    public Game getLastGame() {
        return mLastGame;
    }
}
