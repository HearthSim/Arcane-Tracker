package net.hearthsim.hslog

import net.hearthsim.hslog.parser.achievements.AchievementsParser
import net.hearthsim.hslog.parser.decks.Deck
import net.hearthsim.hslog.parser.power.Game

interface HSLogListener {
    /**
     * called when a game starts, just after the mulligan
     * @param game: the game
     */
    fun onGameStart(game: Game)

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
    fun onRawGame(gameString: String, gameStartMillis: Long)

    /**
     *
     */
    fun onCardGained(cardGained: AchievementsParser.CardGained)

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
}