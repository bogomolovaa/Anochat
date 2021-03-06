package bogomolov.aa.anochat

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsViewTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        ActivityScenario.launch(HiltTestActivity::class.java).onActivity { activity ->
            activity.navController.navigate(R.id.settings_graph)
        }
    }

    @Test
    fun test() {
        onView(withText("Privacy Policy")).check(matches(isDisplayed()))
    }
}

