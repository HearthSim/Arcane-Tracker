package net.mbonnin.arcanetracker.parser;

import android.os.Handler;

import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.DeckList;
import net.mbonnin.arcanetracker.HeroUtilKt;
import net.mbonnin.arcanetracker.MainViewCompanion;
import net.mbonnin.arcanetracker.adapter.Controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ArenaParser implements LogReader.LineConsumer {
    final Pattern DraftManager$OnChosen = Pattern.compile(".*DraftManager.OnChosen\\(\\): hero=(.*) .*");
    final Pattern Client_chooses = Pattern.compile(".*Client chooses: .* \\((.*)\\)");
    private final Handler mHandler;
    private boolean mReadingPreviousData = true;

    public ArenaParser() {
        mHandler = new Handler();
    }
    public void onLine(String line) {
        Timber.v(line);
        Matcher matcher;


        if (!mReadingPreviousData) {
            /*
             * a new ArenaDraft is started
             */
            matcher = DraftManager$OnChosen.matcher(line);
            if (matcher.matches()) {
                int classIndex = HeroUtilKt.heroIdToClassIndex(matcher.group(1));
                Timber.d("new hero: %d", classIndex);

                mHandler.post(() -> newArenaRun(classIndex));
                return;
            }

            /*
             * a card is chosen
             */
            matcher = Client_chooses.matcher(line);
            if (matcher.matches()) {
                String cardId = matcher.group(1);
                if (cardId.toLowerCase().startsWith("hero_")) {
                    // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                    Timber.e("skip hero " + cardId);
                } else {
                    mHandler.post(() -> newArenaCard(cardId));
                }
            }
        }
    }

    private void newArenaCard(String cardId) {
        Deck deck = DeckList.getArenaDeck();
        deck.addCard(cardId, 1);

        Controller.get().setPlayerDeck(deck.cards);

        DeckList.saveArena();
    }

    private void newArenaRun(int classIndex) {
        Deck deck = DeckList.getArenaDeck();
        deck.clear();
        deck.classIndex = classIndex;

        MainViewCompanion.getPlayerCompanion().setDeck(deck);

        Controller.resetAll();

        DeckList.saveArena();
    }


    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
    }
}
