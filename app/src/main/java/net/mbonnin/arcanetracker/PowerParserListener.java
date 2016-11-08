package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.PowerParser;

/**
 * Created by martin on 11/7/16.
 */
public class PowerParserListener implements PowerParser.Listener {
    static PowerParserListener sPowerParserListener;
    static PowerParserListener get() {
        if (sPowerParserListener == null) {
            sPowerParserListener = new PowerParserListener();
        }

        return sPowerParserListener;
    }

    @Override
    public void onNewGame(Game game) {

    }
}
