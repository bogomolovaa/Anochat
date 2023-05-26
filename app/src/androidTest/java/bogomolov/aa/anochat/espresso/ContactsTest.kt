package bogomolov.aa.anochat.espresso

/*
@HiltAndroidTest
class ContactsTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firebase: Firebase

    private var navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    private lateinit var fragment: UsersFragment

    private var action: UserAction? = null

    private val users = listOf(
        User(uid = "uid1", name = "user1", phone = "1234567", status = "status1"),
        User(uid = "uid2", name = "user2", phone = "2345678", status = "status2"),
        User(uid = "uid3", name = "user3", phone = "3456789", status = "status3")
    )

    private val user4 = User(uid = "uid4", name = "user4", phone = "+1234567", status = "status4")

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            for (user in users) Mockito.`when`(firebase.getUser(user.uid)).thenReturn(user)
            Mockito.`when`(firebase.findByPhone(user4.phone!!)).thenReturn(listOf(user4))
            Mockito.`when`(firebase.receiveUsersByPhones(anyList())).thenReturn(users)
            for (user in users) userRepository.getOrAddUser(user.uid)
        }
        fragment = navigateTo(R.id.usersFragment, navController)
        action = fragment.viewModel.addActionListener { action = it }
    }

    @Test
    fun test_search_contact() {
        val query = users[0].name
        onView(withId(R.id.action_search)).perform(click())
        onView(withId(R.id.search_src_text)).perform(ViewActions.replaceText(query))
        onView(withId(R.id.search_src_text)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(query, (action as SearchAction).query)
        onView(withText(users[0].phone)).check(matches(isDisplayed()))
        for (i in 1 until users.size)
            onView(withText(users[1].name)).check(doesNotExist())
    }

    @Test
    fun test_search_by_phone() {
        val query = user4.phone
        onView(withId(R.id.action_search)).perform(click())
        onView(withId(R.id.search_src_text)).perform(ViewActions.replaceText(query))
        onView(withId(R.id.search_src_text)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(query, (action as SearchAction).query)
        onView(withText(user4.name)).check(matches(isDisplayed()))

        onView(withId(R.id.search_close_btn)).perform(click())
        assertThat("action is ResetSearchAction", action is ResetSearchAction)
    }

    @Test
    fun test_users_binding() {
        assertThat("action is LoadContactsAction", action is LoadContactsAction)
        for (user in users) {
            onView(withText(user.name)).check(matches(isDisplayed()))
            onView(withText(user.status)).check(matches(isDisplayed()))
            onView(withText(user.phone)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun test_create_conversation() {
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
        assertEquals(users[0], (action as CreateConversationAction).user)
        assertEquals(R.id.conversationFragment, navController.backStack.last().destination.id)
    }
}
 */