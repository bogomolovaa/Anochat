package bogomolov.aa.anochat.espresso

/*
@HiltAndroidTest
@ExperimentalCoroutinesApi
class ConversationViewTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var readPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

    @get:Rule
    var cameraPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    var recordAudioPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var firebase: Firebase

    @Inject
    lateinit var audioPlayer: AudioPlayer

    private var navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    private lateinit var fragment: ConversationFragment

    private lateinit var user: User

    private lateinit var message: Message


    @Before
    fun setUp() {
        hiltRule.inject()
        emojiSupport()
        runBlocking {
            Mockito.`when`(firebase.getUser(any(String::class.java)))
                .thenReturn(User(uid = "uid", name = "John"))
            user = userRepository.getOrAddUser("uid")
            val conversationId = conversationRepository.createOrGetConversation(user)
            val time = System.currentTimeMillis()
            message = Message(text = "text", conversationId = conversationId, time = time)
            message.id = messageRepository.saveMessage(message)
            Mockito.`when`(firebase.addUserStatusListener(any(),any(), any())).thenReturn(flow {
                emit(Triple(false, true, 0L))
            })
            Mockito.`when`(firebase.sendMessage(uid = user.uid)).thenReturn("messageId")
            Mockito.`when`(audioPlayer.initPlayer(any(), any())).thenReturn(10000)
            Mockito.`when`(audioPlayer.startRecording()).thenReturn("file")
            Mockito.`when`(audioPlayer.startPlay()).thenReturn(true)
            Mockito.`when`(audioPlayer.pausePlay()).thenReturn(true)
        }

        fragment = navigateTo(R.id.conversationFragment, navController,
            Bundle().apply { putLong("id", message.conversationId) })
        Intents.init()
        Dispatchers.setMain(TestCoroutineDispatcher())
        //fragment.viewModel.dispatcher = Dispatchers.Main
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
        Intents.release()
    }

    @Test
    fun test_mapping() {
        assertEquals(
            message.conversationId,
            (action as InitConversationAction).conversationId
        )
        onView(withText(message.text)).check(matches(isDisplayed()))
        onView(withText(user.name)).check(matches(isDisplayed()))
        onView(withText(ONLINE_STATUS)).check(matches(isDisplayed()))
    }

    @Test
    fun test_click_user() {
        onView(withText(user.name)).perform(click())
        assertEquals(R.id.userViewFragment, navController.backStack.last().destination.id)
        assertEquals(user.id, navController.backStack.last().arguments?.get("id") as Long)
    }

    @Test
    fun test_send_message() {
        val text = "new text"
        onView(withId(R.id.message_input_text)).perform(replaceText(text))
        onView(withId(R.id.fab)).perform(click())
        //assertEquals(text, (action as SendMessageAction).text)
        onView(withText(text)).check(matches(isDisplayed()))
    }

    @Test
    fun test_delete_message() {
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )
        onView(withId(R.id.delete_messages_action)).perform(click())
        assertThat("", (action as DeleteMessagesAction).ids.contains(message.id))
        onView(withText(message.text)).check(ViewAssertions.doesNotExist())
    }

    @Test
    fun test_recording() {
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.fabMic)).perform(click())
        assertThat("action is StartRecordingAction", action is StartRecordingAction)
        onView(withId(R.id.fab)).perform(click())
        assertThat("action is StopRecordingAction", action is StopRecordingAction)
    }

    @Test
    fun test_playing() {
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.fabMic)).perform(click())
        onView(withId(R.id.fab)).perform(click())
        onView(allOf(withId(R.id.playPause), isDisplayed())).perform(click())
        assertThat("action is StartPlayingAction", action is StartPlayingAction)
        onView(allOf(withId(R.id.playPause), isDisplayed())).perform(click())
        assertThat("action is PausePlayingAction", action is PausePlayingAction)
    }

    @Test
    fun test_camera() {
        intending(hasAction(ACTION_IMAGE_CAPTURE)).respondWith(Instrumentation.ActivityResult(
            Activity.RESULT_OK, Intent()
        ))
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.fabCamera)).perform(click())
        intended(hasAction(ACTION_IMAGE_CAPTURE))
    }

    @Test
    fun test_file_attachment() {
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(Instrumentation.ActivityResult(
            Activity.RESULT_OK, Intent()
        ))
        onView(withId(R.id.fab)).perform(click())
        onView(withId(R.id.fabFile)).perform(click())
        intended(hasAction(Intent.ACTION_CHOOSER))
    }

    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(ApplicationProvider.getApplicationContext())
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }
}
 */