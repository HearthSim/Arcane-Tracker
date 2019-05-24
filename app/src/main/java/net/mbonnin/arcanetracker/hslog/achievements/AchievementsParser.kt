package net.mbonnin.arcanetracker.hslog.achievements

import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.mbonnin.arcanetracker.CardUtil
import net.mbonnin.arcanetracker.hslog.Console
import net.mbonnin.arcanetracker.room.RDatabaseSingleton
import net.mbonnin.arcanetracker.room.RPack
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AchievementsParser(val console: Console) {
    private val CARD_GAINED = Regex(".*NotifyOfCardGained:.*cardId=(.*) .* (.*) [0-9]*")
    private val cardList = mutableListOf<CardGained>()
    private var disposable: Disposable? = null

    class CardGained(val id: String, val golden: Boolean) {
        override fun toString(): String {
            if (golden) {
                return "$id*"
            } else {
                return id
            }
        }
    }

    fun process(rawLine: String, isOldData: Boolean) {
        if (isOldData) {
            return
        }
        //D 20:44:22.9979440 NotifyOfCardGained: [name=Eviscerate cardId=EX1_124 type=SPELL] NORMAL 3
        console.debug(rawLine)
        val matcher = CARD_GAINED.matchEntire(rawLine)

        if (matcher != null) {
            synchronized(this) {
                val cardId = matcher.groupValues[1]
                val isGolden = matcher.groupValues[2] == "GOLDEN"
                cardList.add(CardGained(cardId, isGolden))
            }

            console.debug("Opened card: ${matcher.groupValues[1]}")

            // if some delay pass without a new card incoming, we consider the pack done
            disposable?.dispose()
            disposable = Completable.complete().delay(2000, TimeUnit.MILLISECONDS)
                    .observeOn(Schedulers.io())
                    .subscribe {
                        synchronized(this) {
                            if (cardList.size == 5) {
                                val dust = cardList.sumBy {
                                    val card = CardUtil.getCard(it.id)
                                    CardUtil.getDust(card.rarity, it.golden)
                                }
                                val rPack = RPack(cardList = cardList.map { it.toString() }.joinToString(","), dust = dust)
                                RDatabaseSingleton.instance.packDao().insert(rPack)
                            } else {
                                Timber.e("wrong number of cards in pack: ${cardList.size}")
                            }
                            cardList.clear()
                        }
                    }
        }
    }

}