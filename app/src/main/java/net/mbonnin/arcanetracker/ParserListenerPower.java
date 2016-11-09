package net.mbonnin.arcanetracker;

import net.mbonnin.arcanetracker.parser.Game;
import net.mbonnin.arcanetracker.parser.PowerParser;

/**
 * Created by martin on 11/7/16.
 */
public class ParserListenerPower implements PowerParser.Listener {
    static ParserListenerPower sParserListenerPower;
    static ParserListenerPower get() {
        if (sParserListenerPower == null) {
            sParserListenerPower = new ParserListenerPower();
        }

        return sParserListenerPower;
    }

    @Override
    public void onNewGame(Game game) {
        MainViewCompanion.getPlayerCompanion().setPlayer(game.player);
        MainViewCompanion.getOpponentCompanion().setPlayer(game.opponent);
    }

    @Override
    public void enEndGame(Game game) {
        game.player.listeners.clear();
        game.opponent.listeners.clear();
    }
}
