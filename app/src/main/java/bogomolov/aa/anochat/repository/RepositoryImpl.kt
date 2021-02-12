package bogomolov.aa.anochat.repository

import android.util.Log
import bogomolov.aa.anochat.domain.Conversation
import bogomolov.aa.anochat.domain.Message
import bogomolov.aa.anochat.domain.Settings
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.repository.entity.ConversationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"

@Singleton
class RepositoryImpl(
    private val db: AppDatabase,
    private val firebase: FirebaseRepository,
    private val keyValueStore: KeyValueStore,
    private val crypto: Crypto,
    private val filesDir: File
) : Repository, IFirebaseRepository by firebase {
    private val mapper = ModelEntityMapper()


    override fun getConversation(id: Long): Conversation =
        mapper.entityToModel(db.conversationDao().loadConversation(id))!!

    override fun sendPublicKey(uid: String, initiator: Boolean) {
        val sentSettingName = getSentSettingName(uid)
        val isSent: Boolean = keyValueStore.getValue<Boolean>(sentSettingName) ?: false
        if (!isSent) {
            val myUid = getMyUID()!!
            val publicKey = crypto.generatePublicKey(uid, myUid)
            if (publicKey != null) {
                firebase.sendMessage(uid = uid, publicKey = publicKey, initiator = initiator)
                keyValueStore.setValue(sentSettingName, true)
                Log.i(TAG, "send publicKey for $uid")
            } else {
                Log.i(TAG, "publicKey not generated for $uid")
            }
        }
    }

    override suspend fun sendMessage(message: Message, uid: String) {
        Log.i("test", "sendMessage message")
        if (message.id == 0L) saveMessage(message)
        val myUid = getMyUID()!!
        val secretKey = crypto.getSecretKey(uid, myUid)
        if (secretKey != null) {
            val file = message.audio ?: message.image
            if (file != null) uploadFile(file, uid, needEncrypt = true)
            val text = crypto.encryptString(secretKey, message.text)
            message.messageId = firebase.sendMessage(
                text,
                message.replyMessage?.messageId,
                message.image,
                message.audio,
                uid
            )
            db.messageDao().updateMessageIdAndSent(message.id, message.messageId)
        } else {
            sendPublicKey(uid, true)
        }
    }

    override suspend fun sendPendingMessages(uid: String) {
        for (message in getPendingMessages(uid)) sendMessage(message, uid)
    }

    private fun getPendingMessages(uid: String): List<Message> {
        val myUid = getMyUID()!!
        val userId = db.userDao().findByUid(uid)?.id
        return if (userId != null)
            mapper.entityToModel(db.messageDao().getNotSent(userId, myUid))
        else listOf()
    }

    private fun saveMessage(message: Message) {
        val entity = mapper.modelToEntity(message)
        entity.myUid = getMyUID()!!
        message.id = db.messageDao().insert(entity)
        Log.i(TAG, "save message ${message.id} $entity")
        db.conversationDao().updateLastMessage(message.id, message.conversationId)
    }

    override fun receiveReport(messageId: String, received: Int, viewed: Int) {
        Log.i(TAG, "receiveReport received $received viewed $viewed")
        db.messageDao().updateReport(messageId, received, viewed)
        if (viewed == 1 || received == -1) firebase.deleteRemoteMessage(messageId)
    }

    override suspend fun receiveMessage(
        text: String,
        uid: String,
        messageId: String,
        replyId: String?,
        image: String?,
        audio: String?
    ) = if (canReceiveMessage(messageId, uid)) {
        Log.i(TAG, "receiveMessage $messageId $text replyId $replyId")
        val conversationEntity = getOrAddConversation(uid)
        val message = Message(
            time = System.currentTimeMillis(),
            conversationId = conversationEntity.id,
            senderId = conversationEntity.userId,
            messageId = messageId,
            image = image,
            audio = audio
        )
        val myUid = getMyUID()!!
        if (text.isNotEmpty()) message.text = crypto.decryptString(text, uid, myUid)
        if (!replyId.isNullOrEmpty()) message.replyMessage =
            mapper.entityToModel(db.messageDao().getByMessageId(replyId))
        saveMessage(message)
        firebase.sendReport(messageId, 1, 0)
        if (message.image != null)
            downloadFile(message.image, uid, needDecrypt = true)
        if (message.audio != null)
            downloadFile(message.audio, uid, needDecrypt = true)
        message
    } else {
        Log.i(TAG, "not received message $messageId from $uid: null secretKey")
        firebase.sendReport(messageId, -1, 0)
        sendPublicKey(uid, true)
        null
    }

    private fun canReceiveMessage(messageId: String, uid: String) =
        crypto.getSecretKey(uid, getMyUID()!!) != null

    override fun getImagesDataSource(userId: Long) = db.messageDao().getImages(userId)

    override suspend fun getMyUser(): User {
        val myUid = getMyUID()!!
        var user = mapper.entityToModel(db.userDao().findByUid(myUid))
        if (user == null) {
            user = firebase.getUser(myUid)
            syncFromRemoteUser(user!!)
        }
        return user
    }

    override suspend fun updateMyUser(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
        if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
        if (user.photo != null && user.photo != savedUser.photo) {
            firebase.updatePhoto(user.uid, user.photo)
            uploadFile(user.photo, user.uid)
            uploadFile(getMiniPhotoFileName(user.photo), user.uid)
        }
        db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
    }

    private suspend fun syncFromRemoteUser(
        user: User,
        saveLocal: Boolean = true,
        loadFullPhoto: Boolean = true
    ) {
        val savedUser = db.userDao().findByUid(user.uid)
        if (savedUser != null) {
            db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
            if ((user.photo != savedUser.photo && user.photo != null)) {
                if (loadFullPhoto) downloadFile(user.photo, user.uid)
                downloadFile(getMiniPhotoFileName(user.photo), user.uid)
            }
        } else {
            if (saveLocal) user.id = db.userDao().add(mapper.modelToEntity(user))
            if (user.photo != null) {
                if (loadFullPhoto) downloadFile(user.photo, user.uid)
                downloadFile(getMiniPhotoFileName(user.photo), user.uid)
            }
        }
    }

    override suspend fun updateUsersInConversations() {
        val conversations = loadAllConversations()
        for (conversation in conversations) {
            val remoteUser = firebase.getUser(conversation.user.uid)
            if (remoteUser != null) syncFromRemoteUser(remoteUser)
        }
    }

    override fun getUser(id: Long): User = mapper.entityToModel(db.userDao().getUser(id))!!

    private suspend fun getOrAddConversation(uid: String): ConversationEntity {
        val myUid = getMyUID()!!
        val userEntity = db.userDao().findByUid(uid)
        val userId = if (userEntity == null) {
            val user = firebase.getUser(uid)
            if (user != null) syncFromRemoteUser(user)
            user?.id ?: 0L
        } else {
            userEntity.id
        }
        var conversationEntity = db.conversationDao().getConversationByUser(userId, myUid)
        if (conversationEntity == null) {
            conversationEntity = ConversationEntity(userId = userId, myUid = myUid)
            conversationEntity.id = db.conversationDao().add(conversationEntity)
        }
        return conversationEntity
    }


    override fun generateSecretKey(publicKey: String, uid: String): Boolean {
        val myUid = getMyUID()!!
        val generated = crypto.generateSecretKey(publicKey, uid, myUid)
        if (generated) {
            Log.i(TAG, "secret key generated, send messages")
            val sentSettingName = getSentSettingName(uid)
            keyValueStore.setValue(sentSettingName, false)
        } else {
            Log.i(TAG, "secret key not generated: privateKey null")
        }
        return generated
    }

    override suspend fun createConversation(user: User) = getOrAddConversation(user.uid).id

    private fun loadAllConversations(): List<Conversation> {
        val myUid = getMyUID()
        return if (myUid == null) listOf()
        else mapper.entityToModel(db.conversationDao().loadAllConversations(myUid))
    }

    override suspend fun searchByPhone(phone: String): List<User> {
        val searchedUsers = firebase.findByPhone(phone)
        for (user in searchedUsers)
            syncFromRemoteUser(user, saveLocal = false, loadFullPhoto = false)
        return searchedUsers
    }

    override suspend fun updateUsersByPhones(phones: List<String>): List<User> {
        val myUid = getMyUID()!!
        val users = if (phones.isNotEmpty())
            firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
        else listOf()
        for (user in users) syncFromRemoteUser(user, loadFullPhoto = false)
        return users
    }

    private fun markAsViewed(messages: List<Message>) {
        val ids = ArrayList<Long>()
        for (message in messages)
            if (!message.isMine() && message.viewed == 0) {
                message.viewed = 1
                firebase.sendReport(message.messageId, 1, 1)
                ids.add(message.id)
            }
        if (ids.size > 0) db.messageDao().updateAsViewed(ids)
    }

    override fun loadMessagesDataSource(conversationId: Long, scope: CoroutineScope) =
        db.messageDao().loadAll(conversationId).map {
            mapper.entityToModel(it)!!
        }.mapByPage {
            scope.launch(Dispatchers.IO) {
                markAsViewed(it)
            }
            it
        }

    override fun loadConversationsDataSource() =
        db.conversationDao().loadConversations(getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    override fun searchMessagesDataSource(search: String) =
        db.messageDao().searchText("%$search%", getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    override fun getUsersByPhonesDataSource(phones: List<String>) =
        db.userDao().getAll(phones, getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    override suspend fun deleteConversationIfNoMessages(conversation: Conversation) {
        val messages = db.messageDao().getMessages(conversation.id)
        if (messages.isEmpty()) db.conversationDao().deleteByIds(setOf(conversation.id))
    }

    override suspend fun deleteMessages(ids: Set<Long>) {
        db.messageDao().deleteByIds(ids)
    }

    override suspend fun deleteConversations(ids: Set<Long>) {
        db.messageDao().deleteByConversationIds(ids)
        db.conversationDao().deleteByIds(ids)
    }

    private fun uploadFile(fileName: String, uid: String, needEncrypt: Boolean = false) {
        val localFile = File(filesDir, fileName)
        val byteArray = if (needEncrypt) crypto.encryptFile(localFile, uid, getMyUID()!!)
        else localFile.readBytes()
        if (byteArray != null) {
            firebase.uploadFile(fileName, uid, byteArray, needEncrypt)
        } else {
            Log.w(TAG, "not uploaded: can't read file $fileName")
        }
    }

    private suspend fun downloadFile(fileName: String, uid: String, needDecrypt: Boolean = false) {
        val localFile = File(filesDir, fileName)
        firebase.downloadFile(fileName, uid, localFile, needDecrypt)
        if (needDecrypt) crypto.decryptFile(localFile, uid, getMyUID()!!)
    }


    companion object {
        private const val NOTIFICATIONS = "notifications"
        private const val SOUND = "sound"
        private const val VIBRATION = "vibration"
    }

    override fun updateSettings(settings: Settings) {
        keyValueStore.setValue(NOTIFICATIONS, settings.notifications)
        keyValueStore.setValue(SOUND, settings.sound)
        keyValueStore.setValue(VIBRATION, settings.vibration)
    }

    override fun getSettings() = Settings(
        notifications = keyValueStore.getValue(NOTIFICATIONS) ?: true,
        sound = keyValueStore.getValue(SOUND) ?: true,
        vibration = keyValueStore.getValue(VIBRATION) ?: true
    )

    private fun getMyUID() = keyValueStore.getValue<String>(UID)

    private fun getSentSettingName(uid: String) = "${getMyUID()!!}${uid}_sent"
}