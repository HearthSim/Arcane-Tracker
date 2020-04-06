package net.mbonnin.arcanetracker.ui.overlay.adapter


import net.hearthsim.hslog.DeckEntry

class Controller {

    val playerAdapter = ItemAdapter()
    val opponentAdapter = ItemAdapter()


    fun onDeckEntries(isPlayer: Boolean, deckEntries: List<DeckEntry>) {
        if (isPlayer) {
            playerAdapter.setList(deckEntries)
        } else {
            opponentAdapter.setList(deckEntries)
        }
    }


    companion object {
        private var sController: Controller? = null

        fun get(): Controller {
            if (sController == null) {
                sController = Controller()
            }

            return sController!!
        }
    }
}
