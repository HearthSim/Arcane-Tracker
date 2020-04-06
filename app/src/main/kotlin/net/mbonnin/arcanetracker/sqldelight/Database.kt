package net.mbonnin.arcanetracker.sqldelight

import com.squareup.sqldelight.android.AndroidSqliteDriver
import net.mbonnin.arcanetracker.ArcaneTrackerApplication

val mainDatabase by lazy {
    val driver = AndroidSqliteDriver(MainDatabase.Schema, ArcaneTrackerApplication.get(), "maindatabase.db")
    MainDatabase(driver)
}