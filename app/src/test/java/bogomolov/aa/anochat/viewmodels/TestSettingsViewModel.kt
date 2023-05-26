package bogomolov.aa.anochat.viewmodels


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

