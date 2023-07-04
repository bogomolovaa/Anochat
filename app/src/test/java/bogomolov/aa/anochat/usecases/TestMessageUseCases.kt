package bogomolov.aa.anochat.usecases

import bogomolov.aa.anochat.MockKeyValueStore
import bogomolov.aa.anochat.domain.Crypto
import bogomolov.aa.anochat.domain.MessageUseCases
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.repositories.ConversationRepository
import bogomolov.aa.anochat.domain.repositories.MessageRepository
import bogomolov.aa.anochat.domain.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

private const val UID1 = "uid1"
private const val UID2 = "uid2"

@ExperimentalCoroutinesApi
class TestMessageUseCases {

    @Test
    fun initial_send_message() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())

        val attachment = ByteArray(1000) { it.toByte() }
        val messageRep1 = MockMessageRepository(UID1) { attachment }
        val messageRep2 = MockMessageRepository(UID2) { messageRep1.attachment }
        val conversationRep1 = mock(ConversationRepository::class.java)
        val conversationRep2 = mock(ConversationRepository::class.java)
        val userRep1 = mock(UserRepository::class.java)
        val userRep2 = mock(UserRepository::class.java)
        val useCases1 = createUseCases(UID1, messageRep1, conversationRep1, userRep1)
        val useCases2 = createUseCases(UID2, messageRep2, conversationRep2, userRep2)
        messageRep1.remoteUseCases = useCases2
        messageRep2.remoteUseCases = useCases1

        val user = User(uid = UID2)
        `when`(userRep2.getOrAddUser(UID1)).thenReturn(user)
        `when`(conversationRep2.createOrGetConversation(user)).thenReturn(1L)


        val text = "Some text"
        val message1 = Message(text = text, image = "some_file.jpg", messageId = "messageId_123")

        useCases1.sendMessage(message1, UID2)
        val message2 = messageRep2.getMessage(message1.messageId)

        assertEquals(text, message2?.text)
        assertEquals(message1.messageId, message2?.messageId)
        assertEquals(message1.image, message2?.image)
        assertEquals(1,  messageRep1.getMessage(message1.messageId)?.received)
        assert(attachment.contentEquals(messageRep2.attachment))
    }

    private fun createUseCases(
        uid: String,
        messageRepository: MessageRepository,
        conversationRepository: ConversationRepository,
        userRepository: UserRepository
    ): MessageUseCases {
        val keyValueStore = MockKeyValueStore(uid)
        return MessageUseCases(
            messageRepository,
            conversationRepository,
            userRepository,
            keyValueStore,
            Crypto(keyValueStore),
            mock(),
            mock()
        )
    }
}