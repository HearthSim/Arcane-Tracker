package net.mbonnin.arcanetracker.ui.overlay.view

import android.view.View
import android.widget.TextView
import net.mbonnin.arcanetracker.R
import net.mbonnin.arcanetracker.ui.overlay.adapter.Controller
import net.hearthsim.hslog.DeckEntry
import net.hearthsim.hslog.parser.power.BattlegroundsBoard
import net.hearthsim.hslog.parser.power.BattlegroundsMinion
import net.hearthsim.hslog.parser.power.Entity
import net.hearthsim.hsmodel.enum.CardId
import net.mbonnin.arcanetracker.ArcaneTrackerApplication

class OpponentDeckCompanion(v: View) : DeckCompanion(v) {
    init {
        settings.visibility = View.GONE
        winLoss.visibility = View.GONE

        recyclerView.adapter = Controller.get().opponentAdapter

        //Controller.get().opponentAdapter.setList(debugList())
        //update()

        (v.findViewById<TextView>(R.id.text)).setText(v.context.getString(R.string.opponent_deck_will_appear))
    }

    private fun debugList(): List<DeckEntry> {
        return listOf(
                DeckEntry.Hero(
                        ArcaneTrackerApplication.get().cardJson.getCard(CardId.THE_RAT_KING),
                        BattlegroundsBoard(opponentHero = Entity(), turn = 2, minions = emptyList())
                ),
                DeckEntry.Hero(
                        ArcaneTrackerApplication.get().cardJson.getCard(CardId.THE_LICH_KING),
                        BattlegroundsBoard(
                                opponentHero = Entity(),
                                currentTurn = 6,
                                turn = 3,
                                minions = listOf(
                                BattlegroundsMinion(CardId.VOIDWALKER,
                                        12,
                                        13,
                                        true,
                                        true
                                ),
                                BattlegroundsMinion(CardId.TIRION_FORDRING,
                                        5,
                                        6,
                                        false,
                                        false
                                )
                        )
                        )
                )
        )
    }
}