package net.hearthsim.hslog

import net.hearthsim.console.Console
import net.hearthsim.hslog.parser.power.BattlegroundState
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hsmodel.CardJson

class ControllerBattlegrounds(console: Console, val cardJson: CardJson) {
    fun getDeckEntries(game: Game, state: BattlegroundState): List<DeckEntry> {
        return state.boards.map {
            DeckEntry.Hero(cardJson.getCard(it.heroCardId), it)
        }
    }
}