package bogomolov.aa.anochat.repository.repositories

import bogomolov.aa.anochat.domain.Settings
import bogomolov.aa.anochat.domain.User
import bogomolov.aa.anochat.repository.*
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "UserRepository"

@Singleton
class UserRepository @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
) {
    private val mapper = ModelEntityMapper()

    private val UID = "uid"
    private fun getMyUID() = keyValueStore.getValue<String>(UID)

    fun getImagesDataSource(userId: Long) = db.messageDao().getImages(userId)

    fun getUsersByPhonesDataSource(phones: List<String>) =
        db.userDao().getAll(phones, getMyUID() ?: "").map {
            mapper.entityToModel(it)!!
        }

    suspend fun updateUsersByPhones(phones: List<String>): List<User> {
        val myUid = getMyUID()!!
        val users = if (phones.isNotEmpty())
            firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
        else listOf()
        for (user in users) syncFromRemoteUser(user, loadFullPhoto = false)
        return users
    }

    suspend fun updateUsersInConversations() {
        val myUid = getMyUID()!!
        val users: List<User> = mapper.entityToModel(db.userDao().getOpenedConversationUsers(myUid))
        for (user in users)
            firebase.getUser(user.uid)?.let { syncFromRemoteUser(it) }
    }

    suspend fun getMyUser(): User {
        val myUid = getMyUID()!!
        var user = mapper.entityToModel(db.userDao().findByUid(myUid))
        if (user == null) {
            user = firebase.getUser(myUid)
            syncFromRemoteUser(user!!)
        }
        return user
    }

    fun getUser(id: Long): User = mapper.entityToModel(db.userDao().getUser(id))!!

    fun updateMyUser(user: User) {
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

    suspend fun searchByPhone(phone: String): List<User> {
        val searchedUsers = firebase.findByPhone(phone)
        for (user in searchedUsers)
            syncFromRemoteUser(user, saveLocal = false, loadFullPhoto = false)
        return searchedUsers
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

}