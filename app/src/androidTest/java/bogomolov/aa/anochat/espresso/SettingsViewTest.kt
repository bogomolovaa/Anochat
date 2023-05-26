package bogomolov.aa.anochat.espresso

/*
@HiltAndroidTest
@ExperimentalCoroutinesApi
class SettingsViewTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    private lateinit var fragment: SettingsFragment

    @Inject
    lateinit var firebase: Firebase

    @Inject
    lateinit var authRepository: AuthRepository

    private var action: UserAction? = null

    private val myUser = User(uid = DEFAULT_MY_UID, name = "Name1", status = "hey", phone = "1234567")

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            Mockito.`when`(firebase.getUser(DEFAULT_MY_UID)).thenReturn(myUser)
            Mockito.`when`(authRepository.getSettings()).thenReturn(Settings.create())
        }
        fragment = navigateTo(R.id.settingsFragment)
        Intents.init()
        Dispatchers.setMain(TestCoroutineDispatcher())
        fragment.viewModel.dispatcher = Dispatchers.Main
        fragment.viewModel.addActionListener { action = it }
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
        Intents.release()
    }

    @Test
    fun test_edit_photo_click() {
        Intents.intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, Intent()
            )
        )
        onView(withId(R.id.edit_photo)).perform(click())
        intended(hasAction(Intent.ACTION_CHOOSER))
    }

    @Test
    fun test_privacy_policy_click() {
        Intents.intending(hasAction(Intent.ACTION_VIEW)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK, Intent()
            )
        )
        onView(withId(R.id.privacyPolicy)).perform(click())
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse(fragment.requireContext().resources.getString(R.string.privacy_policy_url)))
            )
        )
    }

    @Test
    fun test_state_mapping() = runBlockingTest {
        onView(withText(myUser.name)).check(matches(isDisplayed()))
        onView(withText(myUser.status)).check(matches(isDisplayed()))
        onView(withText(myUser.phone)).check(matches(isDisplayed()))
    }

    @Test
    fun test_actions() {
        val text = "text"
        assertEquals(text, enterText(R.id.edit_username, text).name)
        assertEquals(text, enterText(R.id.edit_status, text).status)
        assertEquals(true, checkClickOn(R.id.notificationsSwitch).notifications)
        assertEquals(true, checkClickOn(R.id.vibrationSwitch).vibration)
        assertEquals(true, checkClickOn(R.id.soundSwitch).sound)
        assertEquals(true, checkClickOn(R.id.gallerySwitch).gallery)
    }


    private fun enterText(id: Int, text: String): User {
        onView(withId(id)).perform(click())
        onView(withId(R.id.enter_text)).perform(replaceText(text))
        onView(withId(R.id.save_button)).perform(click())
        val change = (action as UpdateUserAction).change
        return User().change()
    }

    private fun checkClickOn(switchId: Int): Settings {
        onView(withId(switchId)).perform(click())
        val change = (action as ChangeSettingsAction).change
        return Settings.create().change()
    }
}
 */