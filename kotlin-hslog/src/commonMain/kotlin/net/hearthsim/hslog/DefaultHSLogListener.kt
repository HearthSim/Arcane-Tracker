package net.hearthsim.hslog

import net.hearthsim.hslog.parser.achievements.CardGained
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.PossibleSecret

open class DefaultHSLogListener : HSLogListener {
    override fun onSecrets(possibleSecrets: List<PossibleSecret>) {
    }

    override fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>) {

    }

    override fun onGameStart(game: Game) {

    }

    override fun bgHeroesShow(game: Game, entities: List<Entity>) {

    }

    override fun bgHeroesHide() {

    }
    override fun onGameChanged(game: Game) {

    }

    override fun onGameEnd(game: Game) {

    }

    override fun onTurn(game: Game, turn: Int, isPlayer: Boolean) {

    }

    override fun onRawGame(gameString: ByteArray, gameStartMillis: Long) {

    }

    override fun onCardGained(cardGained: CardGained) {

    }

    override fun onDeckFound(deck: Deck, deckString: String, isArena: Boolean) {

    }

    override fun onPlayerDeckChanged(deck: Deck) {

    }

    override fun onOpponentDeckChanged(deck: Deck) {

    }
}