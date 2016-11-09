package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.ArcaneTrackerApplication;
import net.mbonnin.arcanetracker.Card;
import net.mbonnin.arcanetracker.Deck;
import net.mbonnin.arcanetracker.DeckList;
import net.mbonnin.arcanetracker.MainViewCompanion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ArenaParser {
    private final Listener mListener;
    final Pattern DraftManager$OnBegin = Pattern.compile(".* DraftManager.OnBegin - Got new draft deck with ID: (.*)");
    final Pattern DraftManager$OnChose = Pattern.compile(".* DraftManager.OnChosen\\(\\): hero=(.*) premium=NORMAL");
    final Pattern Client_chooses = Pattern.compile(".* Client chooses: .* \\((.*)\\)");
    final Pattern DraftManager$OnChoicesAndContents = Pattern.compile(".* DraftManager.OnChoicesAndContents - Draft deck contains card (.*)");

    public interface Listener {
        void clear();
        void heroDetected(int classIndex);
        void addCard(String cardId);
        void addIfNotAlreadyThere(String cardId);
    }

    public ArenaParser(String file, Listener listener) {
        mListener = listener;

        /**
         * for Arena, we read the whole file again each time because the file is not that big and it allows us to
         * get the arena deck contents
         */
        new LogReader(file, line -> parseArena(line), true);
    }

    private void parseArena(String line) {
        Timber.v(line);

        Matcher matcher = DraftManager$OnBegin.matcher(line);
        if (matcher.matches()) {
            mListener.clear();
            return;
        }

        matcher = DraftManager$OnChose.matcher(line);
        if (matcher.matches()) {
            mListener.heroDetected(Card.heroIdToClassIndex(matcher.group(1)));
            return;
        }

        matcher = Client_chooses.matcher(line);
        if (matcher.matches()) {
            String cardId = matcher.group(1);
            if (cardId.toLowerCase().startsWith("hero_")) {
                // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                Timber.e("skip hero " + cardId);
            } else {
                mListener.addCard(cardId);
            }
        }

        matcher = DraftManager$OnChoicesAndContents.matcher(line);
        if (matcher.matches()) {
            String cardId = matcher.group(1);
            mListener.addIfNotAlreadyThere(cardId);
        }
    }
}
