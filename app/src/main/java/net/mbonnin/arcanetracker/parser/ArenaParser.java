package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Card;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class ArenaParser implements LogReader.LineConsumer {
    private final Listener mListener;
    final Pattern DraftManager$OnBegin = Pattern.compile("DraftManager.OnBegin - Got new draft deck with ID: (.*)");
    final Pattern DraftManager$OnChose = Pattern.compile("DraftManager.OnChosen\\(\\): hero=(.*) premium=NORMAL");
    final Pattern Client_chooses = Pattern.compile("Client chooses: .* \\((.*)\\)");
    final Pattern DraftManager$OnChoicesAndContents = Pattern.compile("DraftManager.OnChoicesAndContents - Draft deck contains card (.*)");

    public interface Listener {
        void clear();
        void arenaDraftStarted(int classIndex);
        void addCard(String cardId);
        void addIfNotAlreadyThere(String cardId);
    }

    public ArenaParser(Listener listener) {
        mListener = listener;
    }

    public void onLine(String rawLine, int seconds, String line) {
        Timber.v(line);

        Matcher matcher = DraftManager$OnBegin.matcher(line);
        if (matcher.matches()) {
            mListener.clear();
            return;
        }

        matcher = DraftManager$OnChose.matcher(line);
        if (matcher.matches()) {
            mListener.arenaDraftStarted(Card.heroIdToClassIndex(matcher.group(1)));
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
