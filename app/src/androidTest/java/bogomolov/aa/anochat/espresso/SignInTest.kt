package bogomolov.aa.anochat.espresso

/*
@HiltAndroidTest
class SignInTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private var navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    private lateinit var fragment: SignInFragment

    @Inject
    lateinit var authRepository: AuthRepository

    private var action: UserAction? = null

    private val phone = "123456"
    private val code = "1234"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            Mockito.`when`(authRepository.sendPhoneNumber(anyString(), any(), any())).then {
                it.getArgument<PhoneVerification>(2).onCodeSent()
            }
            Mockito.`when`(authRepository.verifySmsCode(anyString(), anyString(), any())).then {
                it.getArgument<PhoneVerification>(2).onCodeVerify(code)
                it.getArgument<PhoneVerification>(2).onComplete()
            }
        }
        fragment = navigateTo(R.id.usersFragment, navController)
        action = fragment.viewModel.addActionListener { action = it }
    }

    @Test
    fun test_phone_auth() {
        onView(withId(R.id.phoneInputText)).perform(replaceText(phone))
        onView(withId(R.id.fab)).perform(click())
        assertEquals(phone, (action as SubmitPhoneNumberAction).number)
        onView(withId(R.id.codeInputText)).perform(replaceText(code))
        onView(withId(R.id.fab)).perform(click())
        assertEquals(code, (action as SubmitSmsCodeAction).code)
        assertEquals(R.id.conversationsListFragment, navController.backStack.last().destination.id)
    }
}
 */