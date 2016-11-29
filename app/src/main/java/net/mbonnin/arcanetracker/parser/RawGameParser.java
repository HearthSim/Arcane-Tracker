package net.mbonnin.arcanetracker.parser;

import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.hsreplay.HSReplay;

import java.util.Date;

import timber.log.Timber;

/**
 * Created by martin on 11/29/16.
 */

public class RawGameParser implements LogReader.LineConsumer {
    public RawGameParser() {}

    StringBuilder builder;
    int goldRewardStateCount;
    String matchStart;

    @Override
    public void onLine(String rawLine, int seconds, String line) {
        String s[] = Utils.extractMethod(line);

        if (s == null) {
            Timber.e("Cannot parse line: " + line);
            return;
        }

        if (!s[0].startsWith("GameState")) {
            return;
        }

        line = s[1];
        if (line.contains("CREATE_GAME")) {
            builder = new StringBuilder();
            matchStart = Utils.ISO8601DATEFORMAT.format(new Date());

            Timber.w(matchStart + " - CREATE GAME: " + rawLine);
            goldRewardStateCount = 0;
        }

        if (builder == null) {
            return;
        }

        builder.append(rawLine);
        builder.append('\n');

        if (line.contains("GOLD_REWARD_STATE")) {
            goldRewardStateCount++;
            if (goldRewardStateCount == 2) {
                String game = builder.toString();

                uploadGame(game);
                builder = null;
            }
        }
    }

    private void uploadGame(String game) {
        HSReplay.get().uploadGame(matchStart, game);

    }
}
