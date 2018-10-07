package net.mbonnin.arcanetracker

import android.view.View
import android.view.WindowManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UITest {
    @get:Rule
    var mActivityRule: ActivityTestRule<SettingsActivity> = ActivityTestRule(SettingsActivity::class.java)

    @Before
    fun before() {
        Intents.init()
    }

    @Test
    fun testButtonSize() {
        onView(withId(R.id.licenses))
                .perform(scrollTo(), click())

        intended(hasComponent(LicensesActivity::class.java.getName()))

    }
}