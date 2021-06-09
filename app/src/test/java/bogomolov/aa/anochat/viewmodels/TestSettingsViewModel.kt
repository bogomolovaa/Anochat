package bogomolov.aa.anochat.viewmodels


import bogomolov.aa.anochat.MockitoKotlinTest.Companion.capture
import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.features.settings.SettingsViewModel
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.verify

/*
@ExperimentalCoroutinesApi
class TestSettingsViewModel {

    @Captor
    private lateinit var userCaptor: ArgumentCaptor<User>

    @Mock
    private lateinit var userUseCases: UserUseCases

    @Mock
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(TestCoroutineDispatcher())
        viewModel = SettingsViewModel(userUseCases, authRepository).apply {
            dispatcher = Dispatchers.Main
        }
        Mockito.`when`(authRepository.getSettings()).thenReturn(Settings())
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_UpdateStatusAction() = runBlockingTest {
        val status = "new status"

        viewModel.updateState { copy(user = User()) }
        viewModel.addAction(UpdateUserAction { copy(status = status)})

        verify(userUseCases).updateMyUser(capture(userCaptor))
        assertEquals(status, userCaptor.value.status)
        assertEquals(status, viewModel.currentState.user?.status)
    }
}
 */

