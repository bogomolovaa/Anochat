package bogomolov.aa.anochat

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import bogomolov.aa.anochat.domain.entity.Message
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.repositories.MessageRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class RepositoriesTest {
    private lateinit var db: AppDatabase
    private var myUid = "myUid"
    private val keyValueStore = MockKeyValueStore(myUid)
    private lateinit var messageRepository: MessageRepositoryImpl

    @Mock
    private lateinit var firebase: Firebase

    @Mock
    private lateinit var fileStore: FileStore

    @Before
    fun prepare() {
        MockitoAnnotations.openMocks(this)
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        messageRepository = MessageRepositoryImpl(db, firebase, keyValueStore, fileStore)
    }

    @Test
    fun test() {
        val message = Message(text = "hello world", messageId = "some_message_id")
        messageRepository.saveMessage(message)
        val message2 = messageRepository.getMessage(message.messageId)
        assertEquals(message.text, message2?.text)
    }


}