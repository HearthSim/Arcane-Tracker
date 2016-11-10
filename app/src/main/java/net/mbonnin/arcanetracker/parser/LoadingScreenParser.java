package net.mbonnin.arcanetracker.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class LoadingScreenParser {
    public static final int MODE_PLAY = 0;
    public static final int MODE_ARENA = 1;
    public static final int MODE_OTHER = 2;
    private final Listener mListener;

    public interface Listener {
        void modeChanged(int newMode);
    }

    public LoadingScreenParser(String file, Listener listener) {
        /**
         * we need to read the whole loading screen if we start Arcane Tracker while in the 'tournament' play screen
         * or arena screen already (and not in main menu)
         */
        mListener = listener;
        new LogReader(file, line -> parseLoadingScreen(line), true);
    }

    private void parseLoadingScreen(String line) {
        Timber.v(line);

        Pattern pattern = Pattern.compile(".* LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String prevMode = matcher.group(1);
            String currMode = matcher.group(2);

            if (currMode.equals("GAMEPLAY")) {
                return;
            }

            int newMode = MODE_OTHER;
            if (currMode.equals("DRAFT")) {
                newMode = MODE_ARENA;
            } else if (currMode.equals("TOURNAMENT")) {
                newMode = MODE_PLAY;
            }

            mListener.modeChanged(newMode);
        }
    }

}
