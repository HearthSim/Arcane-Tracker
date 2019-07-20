package net.mbonnin.arcanetracker

import io.paperdb.Paper
import timber.log.Timber

object PaperDb {
    fun <T> write(key: String, value: T) {
        try {
            Paper.book().write(key, value)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun <T> read(key: String): T? {
        try {
            return Paper.book().read(key)
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    fun delete(key: String) {
        try {
            return Paper.book().delete(key)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}