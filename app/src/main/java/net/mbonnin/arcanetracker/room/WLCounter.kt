package net.mbonnin.arcanetracker.room

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

object WLCounter {
    fun watch(id: String): Flowable<RDeck> {
        return RDatabaseSingleton.instance.deckDao().findById(id)
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