package net.mbonnin.arcanetracker.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 11/7/16.
 */

public class LoadingScreenParser implements LogReader.LineConsumer {
    private static LoadingScreenParser sParser;

    public static final String MODE_TOURNAMENT = "TOURNAMENT";
    public static final String MODE_DRAFT = "DRAFT";
    public static final String MODE_GAMEPLAY = "GAMEPLAY";
    public static final String MODE_COLLECTIONMANAGER = "COLLECTIONMANAGER";
    public static final String MODE_PACKOPENING = "PACKOPENING";
    public static final String MODE_FRIENDLY = "FRIENDLY";
    public static final String MODE_ADVENTURE = "ADVENTURE";
    public static final String MODE_HUB = "HUB";
    public static final String MODE_TAVERN_BRAWL = "TAVERN_BRAWL";
    public static final String MODE_UNKNOWN = "UNKNOWN";

    private boolean mReadingPreviousData = true;

    private String mParsedMode;
    private volatile String mMode = MODE_UNKNOWN;
    private volatile String mGameplayMode;

    public static LoadingScreenParser get() {
        if (sParser == null) {
            sParser = new LoadingScreenParser();
        }
        return sParser;
    }

    private LoadingScreenParser() {}

    /*
     * this is called from multiple threads
     * (main thread + screen capture thread)
     * it should be ok to not synchronize it
     */
    public String getGameplayMode(){
        return mGameplayMode;
    }

    public void onLine(String line) {
        Timber.v(line);

        Pattern pattern = Pattern.compile(".*LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String prevMode = matcher.group(1);
            String currMode = matcher.group(2);

            mParsedMode = currMode;
            if (!mReadingPreviousData) {
                /*
                 * do not trigger the mode changes for previous modes, it selects the arena deck at startup always
                 */
                mMode = mParsedMode;
                switch (mMode) {
                    case MODE_DRAFT:
                    case MODE_TOURNAMENT:
                    case MODE_ADVENTURE:
                    case MODE_FRIENDLY:
                    case MODE_TAVERN_BRAWL:
                        mGameplayMode = mMode;
                        break;
                }
            }
        }
    }

    @Override
    public void onPreviousDataRead() {
        mReadingPreviousData = false;
        mMode = mParsedMode;
    }

    public String getMode() {
        return mMode;
    }
}
