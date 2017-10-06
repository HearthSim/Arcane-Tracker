package net.mbonnin.arcanetracker;

import android.os.Handler;

import com.google.firebase.analytics.FirebaseAnalytics;

import net.mbonnin.arcanetracker.detector.DetectorKt;
import net.mbonnin.arcanetracker.hsreplay.HSReplay;
import net.mbonnin.arcanetracker.parser.Entity;
import net.mbonnin.arcanetracker.parser.EntityList;
import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.GameLogic;
import net.mbonnin.arcanetracker.parser.LoadingScreenParser;
import net.mbonnin.arcanetracker.parser.Play;
import net.mbonnin.arcanetracker.trackobot.Trackobot;
import net.mbonnin.arcanetracker.trackobot.model.CardPlay;
import net.mbonnin.arcanetracker.trackobot.model.Result;
import net.mbonnin.arcanetracker.trackobot.model.ResultData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import timber.log.Timber;

import static net.mbonnin.arcanetracker.detector.DetectorKt.MODE_CASUAL;
import static net.mbonnin.arcanetracker.detector.DetectorKt.MODE_RANKED;

public class GameLogicListener implements GameLogic.Listener {

    private static GameLogicListener sGameLogicListener;
    private final Handler mHandler;
    private Game mGame;
    private boolean mGameOver;

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

        for (Integer i : index) {
            Deck candidateDeck = DeckList.get().get(i);

            int score = deckScore(candidateDeck, classIndex, initialCards);

            Timber.i("Deck selection " + candidateDeck.name + " has score " + score);
            if (score > maxScore) {
                bestDeck = candidateDeck;
                maxScore = score;
            }
        }

        if (bestDeck == null) {
            /*
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
    public void gameStarted(Game game) {
        Timber.w("gameStarted");

        Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();
        if (Settings.get(Settings.AUTO_SELECT_DECK, true)) {
            if (LoadingScreenParser.MODE_DRAFT.equals(LoadingScreenParser.get().getGameplayMode())) {
                deck = DeckList.getArenaDeck();
                Timber.w("useArena deck");
            } else {
                int classIndex = game.getPlayer().classIndex();

                /*
                 * we filter the original deck to remove the coin mainly
                 */
                HashMap<String, Integer> map = game.getEntityList(entity -> {
                    return game.getPlayer().entity.EntityID.equals(entity.tags.get(Entity.KEY_CONTROLLER))
                            && Entity.ZONE_HAND.equals(entity.tags.get(Entity.KEY_ZONE))
                            && game.player.entity.PlayerID.equals(entity.extra.originalController);
                })
                        .toCardMap();

                deck = activateBestDeck(classIndex, map);
            }
        }

        MainViewCompanion.getPlayerCompanion().setDeck(deck);

        DeckList.getOpponentDeck().clear();
        DeckList.getOpponentDeck().classIndex = game.getOpponent().classIndex();
        MainViewCompanion.getOpponentCompanion().setDeck(DeckList.getOpponentDeck());

        mGame = game;
        mGameOver = false;

        if (LoadingScreenParser.get().getGameplayMode().equals(LoadingScreenParser.MODE_DRAFT)) {
            mGame.bnetGameType = BnetGameType.BGT_ARENA;
        } else if (LoadingScreenParser.get().getGameplayMode().equals(LoadingScreenParser.MODE_TAVERN_BRAWL)) {
            mGame.bnetGameType = BnetGameType.BGT_TAVERNBRAWL_1P_VERSUS_AI;
        } else if (LoadingScreenParser.get().getGameplayMode().equals(LoadingScreenParser.MODE_ADVENTURE)) {
            mGame.bnetGameType = BnetGameType.BGT_VS_AI;
        } else if (ScreenCaptureResult.INSTANCE.getMode() == MODE_RANKED
                && ScreenCaptureResult.INSTANCE.getFormat() == DetectorKt.FORMAT_STANDARD) {
            mGame.bnetGameType = BnetGameType.BGT_RANKED_STANDARD;
            mGame.rank = ScreenCaptureResult.INSTANCE.getRank();
        } else if (ScreenCaptureResult.INSTANCE.getMode() == MODE_RANKED
                && ScreenCaptureResult.INSTANCE.getFormat() == DetectorKt.FORMAT_WILD) {
            mGame.bnetGameType = BnetGameType.BGT_RANKED_WILD;
            mGame.rank = ScreenCaptureResult.INSTANCE.getRank();
        } else if (ScreenCaptureResult.INSTANCE.getMode() == MODE_CASUAL
                && ScreenCaptureResult.INSTANCE.getFormat() == DetectorKt.FORMAT_STANDARD) {
            mGame.bnetGameType = BnetGameType.BGT_CASUAL_STANDARD_NORMAL;
        } else if (ScreenCaptureResult.INSTANCE.getMode() == MODE_CASUAL
                && ScreenCaptureResult.INSTANCE.getFormat() == DetectorKt.FORMAT_WILD) {
            mGame.bnetGameType = BnetGameType.BGT_CASUAL_WILD;
        } else {
            mGame.bnetGameType = BnetGameType.BGT_UNKNOWN;
        }
    }

    @Override
    public void gameOver() {
        String mode = LoadingScreenParser.get().getGameplayMode();

        Timber.w("gameOver  %s [mode %s] [user %s]", mGame.victory ? "victory" : "lost", mode, Trackobot.get().getUser());

        Deck deck = MainViewCompanion.getPlayerCompanion().getDeck();

        addKnownCardsToDeck(mGame, deck);

        if (mGame.victory) {
            deck.wins++;
        } else {
            deck.losses++;
        }
        MainViewCompanion.getPlayerCompanion().setDeck(deck);

        if (DeckList.ARENA_DECK_ID.equals(deck.id)) {
            DeckList.saveArena();
        } else {
            DeckList.save();
        }

        if ((Utils.isAppDebuggable() || LoadingScreenParser.MODE_DRAFT.equals(mode) || LoadingScreenParser.MODE_TOURNAMENT.equals(mode))
                && Trackobot.get().getUser() != null) {
            ResultData resultData = new ResultData();
            resultData.result = new Result();
            resultData.result.coin = mGame.getPlayer().hasCoin;
            resultData.result.win = mGame.victory;
            resultData.result.mode = Trackobot.getMode(mGame.bnetGameType);
            if (mGame.rank >= 0) {
                resultData.result.rank = mGame.rank;
            }
            resultData.result.hero = Trackobot.getHero(mGame.player.classIndex());
            resultData.result.opponent = Trackobot.getHero(mGame.opponent.classIndex());
            resultData.result.added = Utils.ISO8601DATEFORMAT.format(new Date());

            ArrayList<CardPlay> history = new ArrayList<>();
            for (Play play : mGame.plays) {
                CardPlay cardPlay = new CardPlay();
                cardPlay.player = play.isOpponent ? "opponent" : "me";
                cardPlay.turn = (play.turn + 1) / 2;
                cardPlay.card_id = play.cardId;
                history.add(cardPlay);
            }

            resultData.result.card_history = history;

            Trackobot.get().sendResult(resultData);
        }

        FileTree.get().sync();

        FirebaseAnalytics.getInstance(ArcaneTrackerApplication.getContext()).logEvent("game_ended", null);
        mGameOver = true;
    }

    private void addKnownCardsToDeck(Game game, Deck deck) {

        EntityList originalDeck = game.getEntityList(entity -> game.player.entity.PlayerID.equals(entity.extra.originalController));
        HashMap<String, Integer> originalDeckMap = originalDeck.toCardMap();
        if (Settings.get(Settings.AUTO_ADD_CARDS, true) && Utils.cardMapTotal(deck.cards) < Deck.MAX_CARDS) {
            for (String cardId : originalDeckMap.keySet()) {
                int found = originalDeckMap.get(cardId);
                if (found > Utils.cardMapGet(deck.cards, cardId)) {
                    Timber.w("adding card to the deck " + cardId);
                    deck.cards.put(cardId, found);
                }
            }
            DeckList.save();
        }

    }

    @Override
    public void somethingChanged() {

    }

    private GameLogicListener() {
        mHandler = new Handler();

    }

    public static GameLogicListener get() {
        if (sGameLogicListener == null) {
            sGameLogicListener = new GameLogicListener();
        }

        return sGameLogicListener;
    }

    public void uploadGame(String gameStr, String gameStart) {
        long startTime = System.currentTimeMillis();

        Timber.d("ready to send hsreplay");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mGameOver) {
                    HSReplay.get().uploadGame(gameStart, mGame, gameStr);
                } else if (System.currentTimeMillis() - startTime < 30000) {
                    mHandler.postDelayed(this, 1000);
                } else {
                    Timber.e("timeout waiting for PowerState to finish");
                }
            }
        };

        runnable.run();
    }
}
