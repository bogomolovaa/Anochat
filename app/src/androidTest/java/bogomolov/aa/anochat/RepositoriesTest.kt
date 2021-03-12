package bogomolov.aa.anochat

import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
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

    private val user = User(uid = "uid")

    @Before
    fun prepare() {
        hiltRule.inject()
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(TestCoroutineDispatcher())
        runBlocking {
            Mockito.`when`(fireBase.getUser(user.uid)).thenReturn(user)
        }
    }

    @After
    fun clear() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_deleteConversationIfNoMessages() {
        val conversationId = conversationRepository.createOrGetConversation(user)
        conversationRepository.deleteConversationIfNoMessages(conversationId)
        assertEquals(null, conversationRepository.getConversation(conversationId))
    }

    @Test
    fun test_getPendingMessages() = runBlockingTest {
        userRepository.getOrAddUser(user.uid)
        val conversationId = conversationRepository.createOrGetConversation(user)
        val message = Message(text = "text", conversationId = conversationId)
        message.id = messageRepository.saveMessage(message)
        val messages = messageRepository.getPendingMessages(user.uid)
        assertEquals(message.id, messages[0].id)
    }

    @Test
    fun test_updateUsersInConversations() = runBlockingTest {
        user.id = userRepository.getOrAddUser(user.uid).id
        conversationRepository.createOrGetConversation(user)
        val updatedUser = user.copy(status = "status")
        Mockito.`when`(fireBase.getUser(user.uid)).thenReturn(updatedUser)
        userRepository.updateUsersInConversations()
        assertEquals(updatedUser.status, userRepository.getUser(user.id).status)
    }
}