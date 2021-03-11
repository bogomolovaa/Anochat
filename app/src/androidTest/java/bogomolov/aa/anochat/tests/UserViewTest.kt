package bogomolov.aa.anochat.tests

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.contacts.user.InitUserAction
import bogomolov.aa.anochat.features.contacts.user.UserViewFragment
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.navigateTo
import bogomolov.aa.anochat.repository.Firebase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import javax.inject.Inject

@HiltAndroidTest
class UserViewTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebase: Firebase

    private lateinit var fragment: UserViewFragment

    private var action: UserAction? = null

    private val user = User(uid = "uid", name = "John", phone = "123456", status = "my status")

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            Mockito.`when`(firebase.getUser(user.uid)).thenReturn(user)
            user.id = userRepository.getOrAddUser("uid").id
        }
        fragment =
            navigateTo(R.id.userViewFragment, bundle = Bundle().apply { putLong("id", user.id) })
        action = fragment.viewModel.addActionListener { action = it }
    }

    @Test
    fun test_user_binding() {
        Assert.assertEquals(user.id, (action as InitUserAction).id)
        onView(withText(user.name)).check(matches(isDisplayed()))
        onView(withText(user.phone)).check(matches(isDisplayed()))
        onView(withText(user.status)).check(matches(isDisplayed()))
    }
}