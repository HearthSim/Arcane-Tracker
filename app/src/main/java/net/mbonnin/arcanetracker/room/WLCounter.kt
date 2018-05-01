package net.mbonnin.arcanetracker.room

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class WLCounter(val wins: Int, val losses: Int) {

    companion object {

        fun watch(id: String): Flowable<WLCounter> {
            return RDatabaseSingleton.instance.deckDao().findById(id)
                    .map {
                        it.firstOrNull()?.let {
                            WLCounter(it.wins, it.losses)
                        }
                    }
        }


        fun increment(id: String, wins: Int, losses: Int) {
            Completable.fromAction {
                RDatabaseSingleton.instance.deckDao().incrementWinsLosses(id, wins, losses)
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe({}, Timber::e)
        }

        fun set(id: String, wins: Int, losses: Int) {
            Completable.fromAction {
                RDatabaseSingleton.instance.deckDao().setWinsLosses(id, wins, losses)
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe({}, Timber::e)

        }
    }
}