package net.hearthsim.hslog

class Deck {

    var cards = mutableMapOf<String, Int>()
    var name: String? = null
    var classIndex: Int = 0
    var id: String? = null
    var wins: Int = 0
    var losses: Int = 0


    companion object {
        const val MAX_CARDS = 30
    }
}
