package net.mbonnin.arcanetracker;

import timber.log.Timber;

class TestTree extends Timber.DebugTree {
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        System.out.print(tag + ":" + message + "\n");
    }
}
