package net.arcanetracker.detector

import android.Manifest
import android.support.test.rule.GrantPermissionRule
import org.junit.Rule
import timber.log.Timber

open class BaseTest {

    @Rule
    @JvmField
    val permissionsRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    constructor() {
        Timber.plant(Timber.DebugTree())
    }
}