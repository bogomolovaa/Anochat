package bogomolov.aa.anochat


import bogomolov.aa.anochat.domain.UserUseCases
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.settings.SettingsViewModel
import bogomolov.aa.anochat.features.settings.UpdateStatusAction
import bogomolov.aa.anochat.repository.repositories.AuthRepository
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
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations


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
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_UpdateStatusAction() = runBlockingTest {
        val status = "new status"

        viewModel.setStateAsync { copy(user = User()) }
        viewModel.addAction(UpdateStatusAction(status))

        verify(userUseCases).updateMyUser(capture(userCaptor))
        assertEquals(status, userCaptor.value.status)
        assertEquals(status, viewModel.currentState.user?.status)
    }

    private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}

