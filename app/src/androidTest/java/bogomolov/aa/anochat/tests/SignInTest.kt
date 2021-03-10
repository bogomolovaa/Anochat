package bogomolov.aa.anochat.tests

import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.features.login.SignInFragment
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.navigateTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import javax.inject.Inject

@HiltAndroidTest
class SignInTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private var navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    private lateinit var fragment: SignInFragment

    @Inject
    lateinit var authRepository: AuthRepository

    private var action: UserAction? = null

    private val rightPhone = "123456"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            //Mockito.`when`(authRepository.signIn(rightPhone,any)).thenReturn(users)
        }
        fragment = navigateTo(R.id.usersFragment, navController)
        action = fragment.viewModel.addActionListener { action = it }
    }



}