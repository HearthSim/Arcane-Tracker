package net.mbonnin.arcanetracker.parser

import android.os.Handler
import net.mbonnin.arcanetracker.DeckList
import net.mbonnin.arcanetracker.MainViewCompanion
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.heroIdToClassIndex
import net.mbonnin.hsmodel.Card
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by martin on 11/7/16.
 */

class ArenaParser : LogReader.LineConsumer {
    internal val `DraftManager$OnChosen` = Pattern.compile(".*DraftManager.OnChosen\\(\\): hero=(.*) .*")
    internal val Client_chooses = Pattern.compile(".*Client chooses: .* \\((.*)\\)")
    internal val `DraftManager$OnBegin` = Pattern.compile("DraftManager.OnBegin.*")
    private val mHandler: Handler
    private var mReadingPreviousData = true

    init {
        mHandler = Handler()
    }

    override fun onLine(line: String) {
        Timber.v(line)
        var matcher: Matcher


        if (!mReadingPreviousData) {
            /*
             * a new ArenaDraft is started
             */
            matcher = `DraftManager$OnBegin`.matcher(line)
            if (matcher.matches()) {
                mHandler.post { newArenaRun() }
                return
            }

            matcher = `DraftManager$OnChosen`.matcher(line)
            if (matcher.matches()) {
                val classIndex = heroIdToClassIndex(matcher.group(1))
                Timber.d("new hero: %d", classIndex)

                mHandler.post { setArenaHero(classIndex) }
                return
            }

            /*
             * a card is chosen
             */
            matcher = `DraftManager$OnChosen`.matcher(line)
            if (matcher.matches()) {
                val cardId = matcher.group(1)
                if (cardId.toLowerCase().startsWith("hero_")) {
                    // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                    Timber.e("skip hero " + cardId)
                } else {
                    mHandler.post { newArenaCard(cardId) }
                }
            }
        }
    }

    private fun setArenaHero(classIndex: Int) {
        val deck = DeckList.getArenaDeck()
        deck.clear()
        deck.classIndex = classIndex

        MainViewCompanion.getPlayerCompanion().deck = deck

        Controller.resetAll()

        DeckList.saveArena()
    }

    private fun newArenaCard(cardId: String) {
        val deck = DeckList.getArenaDeck()
        deck.addCard(cardId, 1)

        Controller.get().setPlayerDeck(deck.cards)

        DeckList.saveArena()
    }

    private fun newArenaRun() {
        setArenaHero(Card.CLASS_INDEX_NEUTRAL)
    }


    override fun onPreviousDataRead() {
        mReadingPreviousData = false
    }
}
