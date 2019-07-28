package net.mbonnin.arcanetracker.ui.overlay.adapter


import android.os.Handler
import android.text.TextUtils
import net.mbonnin.arcanetracker.*
import net.hearthsim.hslog.parser.power.Game
import net.hearthsim.hslog.*

class Controller {

    val playerAdapter = ItemAdapter()
    val opponentAdapter = ItemAdapter()


    fun onDeckEntries(game: Game?, isPlayer: Boolean, deckEntries: List<DeckEntry>) {
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
