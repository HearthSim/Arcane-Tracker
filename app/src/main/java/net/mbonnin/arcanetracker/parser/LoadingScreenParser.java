package net.mbonnin.arcanetracker.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class LoadingScreenParser implements LogReader.LineConsumer {
    public static final int MODE_PLAY = 0;
    public static final int MODE_ARENA = 1;
    public static final int MODE_OTHER = 2;
    private final Listener mListener;
    private boolean mReadingPreviousData = true;
    private int mCurrentMode;

    public static String friendlyMode(int mode) {
        switch (mode) {
            case MODE_ARENA: return "ARENA";
            case MODE_PLAY: return "PLAY";
            case MODE_OTHER: return "OTHER";
            default: return "?";
        }
    }

    public interface Listener {
        void modeChanged(int newMode);
    }

    public LoadingScreenParser(Listener listener) {
        mListener = listener;
    }

    public void onLine(String line) {
        Timber.v(line);

        Pattern pattern = Pattern.compile(".*LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");
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

            if (!mReadingPreviousData) {
                /**
                 * do not trigger the mode changes for previous modes, it selects the arena deck at startup always
                 */
                mListener.modeChanged(newMode);
            }
            mCurrentMode = newMode;
        }
    }

    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
        mListener.modeChanged(mCurrentMode);
    }
}
