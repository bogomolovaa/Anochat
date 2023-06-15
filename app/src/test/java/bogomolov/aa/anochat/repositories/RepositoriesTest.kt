package bogomolov.aa.anochat.repositories

import bogomolov.aa.anochat.DEFAULT_MY_UID
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class RepositoriesTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var messageRepository: MessageRepositoryImpl

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var fireBase: Firebase

    @Inject
    lateinit var dispatcher: CoroutineDispatcher

    private val user = User(uid = DEFAULT_MY_UID)

    @Before
    fun prepare() {
        hiltRule.inject()
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(dispatcher)
        runBlocking {
            Mockito.`when`(fireBase.getUser(user.uid)).thenReturn(user)
        }
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_deleteConversationIfNoMessages() = runTest {
        user.id = userRepository.getOrAddUser(user.uid).id
        val conversationId = conversationRepository.createOrGetConversation(user)
        conversationRepository.deleteConversationIfNoMessages(conversationId)
        advanceUntilIdle()
        assertEquals(null, conversationRepository.getConversation(conversationId))
    }

    @Test
    fun test_getPendingMessages() = runTest {
        userRepository.getOrAddUser(user.uid)
        val conversationId = conversationRepository.createOrGetConversation(user)
        val message = Message(text = "text", conversationId = conversationId, isMine = true)
        val id = messageRepository.saveMessage(message)
        val messages = messageRepository.getPendingMessages(user.uid)
        assertEquals(id, messages[0].id)
    }

    @Test
    fun test_updateUsersInConversations() = runTest {
        user.id = userRepository.getOrAddUser(user.uid).id
        conversationRepository.createOrGetConversation(user)
        val updatedUser = user.copy(status = "status")
        Mockito.`when`(fireBase.getUser(user.uid)).thenReturn(updatedUser)
        userRepository.updateUsersInConversations(true)
        assertEquals(updatedUser.status, userRepository.getUser(user.id).status)
    }
}