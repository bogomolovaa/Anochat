package bogomolov.aa.anochat.espresso

/*
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
 */