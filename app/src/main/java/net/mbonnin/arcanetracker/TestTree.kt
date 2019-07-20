package net.mbonnin.arcanetracker

import timber.log.Timber

internal class TestTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        print("$tag:$message\n")
    }
}
