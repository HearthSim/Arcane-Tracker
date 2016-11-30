package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.DeckList;
import net.mbonnin.arcanetracker.MainViewCompanion;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.adapter.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ArenaParser implements LogReader.LineConsumer {
    final Pattern DraftManager$OnChosen = Pattern.compile(".*DraftManager.OnChosen\\(\\): hero=(.*) premium=NORMAL");
    final Pattern Client_chooses = Pattern.compile(".*Client chooses: .* \\((.*)\\)");
    private boolean mReadingPreviousData = true;

    public void onLine(String line) {
        Timber.v(line);
        Matcher matcher;


        if (!mReadingPreviousData) {
            /**
             * a new ArenaDraft is started
             */
            matcher = DraftManager$OnChosen.matcher(line);
            if (matcher.matches()) {
                int classIndex = Card.heroIdToClassIndex(matcher.group(1));
                Timber.d("new hero: %d", classIndex);

                Deck deck = DeckList.getArenaDeck();
                deck.clear();
                deck.classIndex = classIndex;

                MainViewCompanion.getPlayerCompanion().setDeck(deck);

                Controller.resetAll();

                DeckList.saveArena();
                return;
            }

            /**
             * a card is chosen
             */
            matcher = Client_chooses.matcher(line);
            if (matcher.matches()) {
                String cardId = matcher.group(1);
                if (cardId.toLowerCase().startsWith("hero_")) {
                    // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                    Timber.e("skip hero " + cardId);
                } else {
                    Deck deck = DeckList.getArenaDeck();
                    deck.addCard(cardId, 1);

                    Controller.getPlayerController().setDeck(deck);

                    DeckList.saveArena();
                }
            }
        }
    }


    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
    }
}
