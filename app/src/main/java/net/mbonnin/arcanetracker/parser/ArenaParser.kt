package net.mbonnin.arcanetracker.parser

import android.os.Handler
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.DeckList
import net.mbonnin.arcanetracker.MainViewCompanion
import net.mbonnin.arcanetracker.adapter.Controller
import net.mbonnin.arcanetracker.heroIdToClassIndex
import net.mbonnin.hsmodel.Card
import net.mbonnin.hsmodel.Type
import timber.log.Timber
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by martin on 11/7/16.
 */

class ArenaParser : LogReader.LineConsumer {
    internal val DraftManagerOnChosen = Pattern.compile(".*DraftManager.OnChosen\\(\\): hero=(.*) .*")
    internal val Client_chooses = Pattern.compile(".*Client chooses: .* \\((.*)\\)")
    internal val DraftManagerOnBegin = Pattern.compile(".*DraftManager.OnBegin.*")
    val SetDrafMode = Pattern.compile(".*SetDraftMode - (.*)")

    private val mHandler: Handler
    private var mReadingPreviousData = true
    private var parsingDraftMode: String = DRAFT_MODE_UNKNOWN

    @Volatile var draftMode: String = DRAFT_MODE_UNKNOWN
        private set

    init {
        mHandler = Handler()
    }


    private constructor ()

    override fun onLine(rawLine: String) {
        Timber.v(rawLine)
        var matcher: Matcher

        matcher = SetDrafMode.matcher(rawLine)
        if (matcher.matches()) {
            if (mReadingPreviousData) {
                parsingDraftMode = matcher.group(1)
            } else {
                draftMode = matcher.group(1)
            }
            return
        }

        if (!mReadingPreviousData) {
            /*
             * a new ArenaDraft is started
             */
            matcher = DraftManagerOnBegin.matcher(rawLine)
            if (matcher.matches()) {
                mHandler.post { newArenaRun() }
                return
            }

            matcher = DraftManagerOnChosen.matcher(rawLine)
            if (matcher.matches()) {
                val classIndex = heroIdToClassIndex(matcher.group(1))
                Timber.d("new hero: %d", classIndex)

                mHandler.post { setArenaHero(classIndex) }
                return
            }

            /*
             * a card is chosen
             */
            matcher = Client_chooses.matcher(rawLine)
            if (matcher.matches()) {
                val cardId = matcher.group(1)
                val card = CardUtil.getCard(cardId)
                if (Type.HERO.equals(card.type)
                        || Type.HERO_POWER.equals(card.type)) {
                    // This must be a hero ("Client chooses: Tyrande Whisperwind (HERO_09a)")
                    Timber.d("skip hero or hero_power" + cardId)
                } else {
                    mHandler.post { newArenaCard(cardId) }
                }
                return
            }
        }
    }

    private fun setArenaHero(classIndex: Int) {
        val deck = DeckList.getArenaDeck()
        deck.clear()
        deck.classIndex = classIndex

        Timber.d("setArenaHero %d", classIndex)

        MainViewCompanion.playerCompanion.deck = deck

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
        draftMode = parsingDraftMode
    }

    companion object {
        const val DRAFT_MODE_UNKNOWN = "UNKNOWN"
        const val DRAFT_MODE_ACTIVE_DRAFT_DECK = "ACTIVE_DRAFT_DECK"
        const val DRAFT_MODE_IN_REWARDS = "IN_REWARDS"
        const val DRAFT_MODE_DRAFTING = "DRAFTING"
        const val DRAFT_MODE_NO_ACTIVE_DRAFT = "NO_ACTIVE_DRAFT"

        private var arenaParser: ArenaParser? = null

        fun get(): ArenaParser {
            /*
             * this is not thread safe but should be ok no matter what since get() is called from Application.onCreate()
             */
            if (arenaParser == null) {
                arenaParser = ArenaParser()
            }

            return arenaParser!!
        }
    }
}
