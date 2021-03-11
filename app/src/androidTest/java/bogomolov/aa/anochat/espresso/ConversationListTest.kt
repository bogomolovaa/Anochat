package bogomolov.aa.anochat.espresso

import android.view.KeyEvent
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import bogomolov.aa.anochat.MockitoKotlinAndroidTest.Companion.any
import bogomolov.aa.anochat.R
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.conversations.list.ConversationListFragment
import bogomolov.aa.anochat.features.conversations.list.DeleteConversationsAction
import bogomolov.aa.anochat.features.conversations.list.InitConversationsAction
import bogomolov.aa.anochat.features.conversations.list.SignOutAction
import bogomolov.aa.anochat.features.shared.AuthRepository
import bogomolov.aa.anochat.features.shared.mvi.UserAction
import bogomolov.aa.anochat.getStringRes
import bogomolov.aa.anochat.navigateTo
import bogomolov.aa.anochat.repository.Firebase
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import javax.inject.Inject

@HiltAndroidTest
class ConversationListTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var firebase: Firebase

    private var navController = TestNavHostController(ApplicationProvider.getApplicationContext())

    private lateinit var fragment: ConversationListFragment

    private var action: UserAction? = null

    private val text = "Hello"
    private val user = User(name = "John")
    private var conversationId = 0L

    @Before
    fun setUp() {
        hiltRule.inject()
        emojiSupport()
        runBlocking {
            Mockito.`when`(firebase.getUser(any(String::class.java))).thenReturn(user)
            val user = userRepository.getOrAddUser("uid")
            conversationId = conversationRepository.createOrGetConversation(user)
            val time = System.currentTimeMillis()
            val lastMessage = Message(text = text, conversationId = conversationId, time = time)
            messageRepository.saveMessage(lastMessage)
        }

        fragment = navigateTo(R.id.settingsFragment, navController)
        fragment.viewModel.dispatcher = Dispatchers.Main
        action = fragment.viewModel.addActionListener { action = it }
    }

    @Test
    fun test_delete_action() {
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick())
        )
        onView(withId(R.id.delete_conversations_action)).perform(click())
        assertThat("", (action as DeleteConversationsAction).ids.contains(conversationId))
        onView(withText(text)).check(doesNotExist())
    }

    @Test
    fun test_conversation_binding() {
        assertThat("action is InitConversationsAction", action is InitConversationsAction)
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0)
        )
        onView(withText(user.name)).check(matches(isDisplayed()))
        onView(withText(text)).check(matches(isDisplayed()))
    }

    @Test
    fun test_search() {
        onView(withId(R.id.search_messages_action)).perform(click())
        onView(withId(R.id.search_src_text)).perform(replaceText(text))
        onView(withId(R.id.search_src_text)).perform(pressKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(R.id.messageSearchFragment, navController.backStack.last().destination.id)
        assertEquals(text, navController.backStack.last().arguments?.get("search") as String)
    }

    @Test
    fun test_click_dialog() {
        onView(withId(R.id.recyclerView)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
        assertEquals(R.id.conversationFragment, navController.backStack.last().destination.id)
        assertEquals(1, navController.backStack.last().arguments?.get("id") as Long)
    }

    @Test
    fun test_navigate_to_settings() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        onView(withText(getStringRes(R.string.settings))).perform(click())
        assertEquals(R.id.settingsFragment, navController.backStack.last().destination.id)
    }

    @Test
    fun test_navigate_to_contacts() {
        onView(withId(R.id.fab)).perform(click())
        assertEquals(R.id.usersFragment, navController.backStack.last().destination.id)
    }

    @Test
    fun test_sign_out() {
        openActionBarOverflowOrOptionsMenu(ApplicationProvider.getApplicationContext())
        onView(withText(getStringRes(R.string.sign_out))).perform(click())
        assertThat("action is SignOutAction", action is SignOutAction)
        assertEquals(R.id.signInFragment, navController.backStack.last().destination.id)
    }


    private fun emojiSupport() {
        val config = BundledEmojiCompatConfig(ApplicationProvider.getApplicationContext())
        EmojiCompat.init(config)
        EmojiManager.install(IosEmojiProvider())
    }
}

