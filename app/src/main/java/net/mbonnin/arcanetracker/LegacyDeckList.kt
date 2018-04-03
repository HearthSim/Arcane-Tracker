package net.mbonnin.arcanetracker

import java.util.*

object LegacyDeckList : DeckList {

    val ARENA_DECK_ID = "ARENA_DECK_ID"
    private var sList: ArrayList<Deck>? = null
    internal val KEY_LIST = "list"
    internal val KEY_ARENA_DECK = "arena_deck"
    private var sArenaDeck: Deck? = null
    private var sOpponentDeck: Deck? = null

    val arenaDeck: Deck
        get() {
            if (sArenaDeck == null) {
                sArenaDeck = PaperDb.read<Deck>(KEY_ARENA_DECK)
                if (sArenaDeck == null) {
                    sArenaDeck = Deck()
                    sArenaDeck!!.id = ARENA_DECK_ID
                    sArenaDeck!!.name = ArcaneTrackerApplication.context.getString(R.string.arenaDeck)
                }
            }
            sArenaDeck!!.checkClassIndex()
            return sArenaDeck!!
        }

    val opponentDeck: Deck
        get() {
            if (sOpponentDeck == null) {
                sOpponentDeck = Deck()
                sOpponentDeck!!.name = ArcaneTrackerApplication.context.getString(R.string.opponentsDeck)
            }
            return sOpponentDeck!!
        }

    fun createDeck(classIndex: Int): Deck {
        val deck = Deck()
        deck.classIndex = classIndex
        deck.id = UUID.randomUUID().toString()
        deck.name = ArcaneTrackerApplication.context.getString(R.string.yourDeck)

        get().add(deck)

        save()
        return deck
    }

    fun addDeck(deck: Deck) {
        sList!!.add(deck)
        save()
    }

    fun deleteDeck(deck: Deck) {
        sList!!.remove(deck)
        save()
    }

    fun get(): ArrayList<Deck> {
        if (sList == null) {
            sList = PaperDb.read<ArrayList<Deck>>(KEY_LIST)
        }
        if (sList == null) {
            sList = ArrayList()
            save()
        }

        return sList!!
    }

    fun save() {
        PaperDb.write<ArrayList<Deck>>(KEY_LIST, sList!!)
    }

    fun saveArena() {
        PaperDb.write(KEY_ARENA_DECK, arenaDeck)
    }

    fun saveDeck(deck: Deck) {
        if (ARENA_DECK_ID == deck.id) {
            saveArena()
        } else {
            save()
        }
    }

    fun hasValidDeck(): Boolean {
        for (deck in get()) {
            if (deck.cardCount > 0 && !deck.isArena) {
                return true
            }
        }

        return true
    }

    override fun getAllDecks(): List<Deck> {
        val list = get().toMutableList()

        list.add(arenaDeck)

        return list
    }

}
