package net.hearthsim.hslog

import net.hearthsim.hslog.parser.achievements.CardGained
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.parser.power.PossibleSecret

interface HSLogListener {
    /**
     * called when a game starts, just after the mulligan
     * @param game: the game
     */
    fun onGameStart(game: Game)

    fun bgHeroesShow(game: Game, entities: List<Entity>)
    fun bgHeroesHide()

    /**
     * called when a new entity has been revealed, a play has been done or something else changed
     * @param game: the game
     */
    fun onGameChanged(game: Game)

    /**
     * called when the game ends
     * @param game: the game
     */
    fun onGameEnd(game: Game)

    /**
     *
     */
    fun onRawGame(gameString: ByteArray, gameStartMillis: Long)

    /**
     *
     */
    fun onCardGained(cardGained: CardGained)

    /**
     *
     */
    fun onDeckFound(deck: Deck, deckString: String, isArena: Boolean)

    /**
     * called when the player deck is detected
     *      - from Decks.log in ranked/casual/arena/practice
     *      - guessed from the initial mulligan cards in the Zayle/Whizbang cases
     *      - empty for adventures/dungeons and other solo modes
     * @param deck: the new deck
     */
    fun onPlayerDeckChanged(deck: Deck)

    /**
     * called to reset the opponent deck to an empty deck at the start of a game
     * @param deck: the new deck
     */
    fun onOpponentDeckChanged(deck: Deck)

    /**
     * Use this for the turn timer.
     *
     * The timeout may change during a turn so it's not part of this api
     */
    fun onTurn(game: Game, turn: Int, isPlayer: Boolean)

    /**
     * Use this to set the deck entries
     *
     */
    fun onDeckEntries(game: Game, isPlayer: Boolean, deckEntries: List<DeckEntry>)

    /**
     *
     *
     */
    fun onSecrets(possibleSecrets: List<PossibleSecret>)
}