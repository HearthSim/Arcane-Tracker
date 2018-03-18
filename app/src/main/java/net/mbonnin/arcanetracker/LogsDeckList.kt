package net.mbonnin.arcanetracker

object LogsDeckList {
    val sList = ArrayList<Deck>()

    fun clear() {
        sList.clear()
    }

    fun addDeck(deck: Deck) {
        sList.add(deck)
    }
    fun get(): ArrayList<Deck> {
        return sList
    }

}