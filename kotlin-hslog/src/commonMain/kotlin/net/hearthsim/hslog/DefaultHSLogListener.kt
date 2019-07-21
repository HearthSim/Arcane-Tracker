package net.hearthsim.hslog

import net.hearthsim.hslog.parser.achievements.AchievementsParser
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.Game

open class DefaultHSLogListener : HSLogListener {
    override fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>) {

    }

    override fun onGameStart(game: Game) {

    }

    override fun onGameChanged(game: Game) {

    }

    override fun onGameEnd(game: Game) {

    }

    override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {

    }

    override fun onRawGame(gameString: String, gameStartMillis: Long) {

    }

    override fun onCardGained(cardGained: AchievementsParser.CardGained) {

    }

    override fun onDeckFound(deck: Deck, deckString: String, isArena: Boolean) {

    }

    override fun onPlayerDeckChanged(deck: Deck) {

    }

    override fun onOpponentDeckChanged(deck: Deck) {

    }
}