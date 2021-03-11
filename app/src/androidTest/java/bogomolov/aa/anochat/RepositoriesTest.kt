package bogomolov.aa.anochat

import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockitoAnnotations
import javax.inject.Inject

@HiltAndroidTest
class RepositoriesTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var messageRepository: MessageRepositoryImpl

    @Before
    fun prepare() {
        hiltRule.inject()
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun test_saveMessage() {
        val message = Message(text = "hello world", messageId = "some_message_id")
        messageRepository.saveMessage(message)
        val message2 = messageRepository.getMessage(message.messageId)
        assertEquals(message.text, message2?.text)
    }

    @Test
    fun test_deleteConversationIfNoMessages(){

    }

    @Test
    fun test_getPendingMessages(){

    }

    @Test
    fun test_updateUsersInConversations(){

    }
}