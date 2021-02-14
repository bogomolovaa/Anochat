package bogomolov.aa.anochat


import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.features.settings.SettingsViewModel
import bogomolov.aa.anochat.features.settings.UpdateStatusAction
import bogomolov.aa.anochat.repository.Repository
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class TestSettingsActions {

    @Captor
    private lateinit var userCaptor: ArgumentCaptor<User>
    @Mock
    private lateinit var repository: Repository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(TestCoroutineDispatcher())
        viewModel = SettingsViewModel(repository).apply { dispatcher = Dispatchers.Main }
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

        verify(repository).updateMyUser(capture(userCaptor))
        assertEquals(status, userCaptor.value.status)
        assertEquals(status, viewModel.currentState.user?.status)
    }

    private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}

