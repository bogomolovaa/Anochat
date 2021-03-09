package bogomolov.aa.anochat

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.settings.SettingsFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsViewTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var fragment: SettingsFragment

    @Before
    fun setUp() {
        hiltRule.inject()
        fragment = navigateTo(R.id.settingsFragment)
    }

    @Test
    fun test() {
        val status = "hey"
        val user = User(status = status)
        runBlocking {
            fragment.viewModel.setState { copy(user = user) }
        }
        onView(withText(status)).check(matches(isDisplayed()))

    }
}