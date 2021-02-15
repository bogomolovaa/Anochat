package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.Settings
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.getValue
import bogomolov.aa.anochat.domain.setValue
import bogomolov.aa.anochat.repository.*
import kotlinx.coroutines.CoroutineScope
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore
) {
    private val filesDir : String = keyValueStore.getValue(FILES_DIRECTORY)!!
    private val mapper = ModelEntityMapper()


    fun getImagesDataSource(userId: Long) = db.messageDao().getImages(userId)

    fun getUsersByPhonesDataSource(phones: List<String>) =
        db.userDao().getAll(phones, getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    suspend fun updateUsersByPhones(phones: List<String>) =
        if (phones.isNotEmpty()) {
            val myUid = getMyUID()!!
            firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
                .onEach { user -> updateLocalUserFromRemote(user, loadFullPhoto = false) }
        } else listOf()

    suspend fun updateUsersInConversations() {
        val myUid = getMyUID()!!
        val users = mapper.entityToModel<User>(db.userDao().getOpenedConversationUsers(myUid))
        users.forEach { user -> firebase.getUser(user.uid)?.also { updateLocalUserFromRemote(it) } }
    }

    suspend fun getMyUser(): User {
        val myUid = getMyUID()!!
        return getOrAddUser(myUid)
    }

    fun getUser(id: Long): User = mapper.entityToModel(db.userDao().getUser(id))!!

    suspend fun updateMyUser(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
        if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
        if (user.photo != null && user.photo != savedUser.photo) {
            uploadFile(user.photo, user.uid)
            uploadFile(getMiniPhotoFileName(user.photo), user.uid)
            firebase.updatePhoto(user.uid, user.photo)
        }
        db.userDao().updateUser(user.uid, user.phone, user.name, user.photo, user.status)
    }

    suspend fun searchByPhone(phone: String) =
        firebase.findByPhone(phone).onEach { user ->
            updateLocalUserFromRemote(user, saveLocal = false, loadFullPhoto = false)
        }

    fun addUserStatusListener(uid: String, scope: CoroutineScope) =
        firebase.addUserStatusListener(uid, scope)


    companion object {
        private const val NOTIFICATIONS = "notifications"
        private const val SOUND = "sound"
        private const val VIBRATION = "vibration"
    }

    fun updateSettings(settings: Settings) {
        keyValueStore.setValue(NOTIFICATIONS, settings.notifications)
        keyValueStore.setValue(SOUND, settings.sound)
        keyValueStore.setValue(VIBRATION, settings.vibration)
    }

    fun getSettings() = Settings(
        notifications = keyValueStore.getValue(NOTIFICATIONS) ?: true,
        sound = keyValueStore.getValue(SOUND) ?: true,
        vibration = keyValueStore.getValue(VIBRATION) ?: true
    )



    suspend fun getOrAddUser(uid: String): User {
        val userEntity = db.userDao().findByUid(uid)
        return mapper.entityToModel(userEntity) ?: firebase.getUser(uid)!!
            .also { updateLocalUserFromRemote(it) }
    }



    private suspend fun updateLocalUserFromRemote(
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

    private suspend fun downloadFile(fileName: String, uid: String) =
        firebase.downloadFile(fileName, uid, File(filesDir, fileName))

    private suspend fun uploadFile(fileName: String, uid: String) =
        firebase.uploadFile(fileName, uid, File(filesDir, fileName).readBytes())

    private fun getMyUID() = keyValueStore.getMyUID()
}