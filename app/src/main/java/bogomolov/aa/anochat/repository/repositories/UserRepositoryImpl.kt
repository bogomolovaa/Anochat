package bogomolov.aa.anochat.repository.repositories

import android.content.Context
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.fileExists
import bogomolov.aa.anochat.features.shared.getByteArray
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    @ApplicationContext private val context: Context
) : UserRepository {
    private val mapper = ModelEntityMapper()

    override fun getImagesDataSource(userId: Long) = db.messageDao().getImages(userId)

    override fun getUsersByPhonesDataSource(phones: List<String>) =
        db.userDao().getAll(phones, getMyUID()!!).map { mapper.entityToModel(it)!! }

    override suspend fun updateUsersByPhones(phones: List<String>) =
        if (phones.isNotEmpty()) {
            val myUid = getMyUID()!!
            firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
                .onEach { user -> updateLocalUserFromRemote(user, loadFullPhoto = false) }
        } else listOf()

    override suspend fun updateUsersInConversations() {
        val myUid = getMyUID() ?: return
        mapper.entityToModel<User>(db.userDao().getOpenedConversationUsers(myUid)).forEach { user ->
            firebase.getUser(user.uid)?.also { updateLocalUserFromRemote(it) }
        }
    }

    override suspend fun getMyUser() = getOrAddUser(getMyUID()!!)

    override fun getUser(id: Long) = mapper.entityToModel(db.userDao().getUser(id))!!

    override suspend fun updateMyUser(user: User) {
        val savedUser = db.userDao().getUser(user.id)
        if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
        if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
        if (user.photo != null && user.photo != savedUser.photo) {
            uploadFile(user.photo, user.uid)
            uploadFile(getMiniPhotoFileName(user.photo), user.uid)
            firebase.updatePhoto(user.uid, user.photo)
        }
        db.userDao().update(mapper.modelToEntity(user))
    }

    override suspend fun searchByPhone(phone: String) =
        firebase.findByPhone(phone).onEach { user ->
            updateLocalUserFromRemote(user, saveLocal = false, loadFullPhoto = false)
        }

    override fun addUserStatusListener(uid: String, scope: CoroutineScope) =
        firebase.addUserStatusListener(uid, scope)

    override suspend fun getOrAddUser(uid: String): User {
        val userEntity = db.userDao().findByUid(uid)
        val user = mapper.entityToModel(userEntity) ?: firebase.getUser(uid)!!
        return user.also { updateLocalUserFromRemote(it) }
    }


    private suspend fun updateLocalUserFromRemote(
        user: User,
        saveLocal: Boolean = true,
        loadFullPhoto: Boolean = true
    ) {
        val savedUser = db.userDao().findByUid(user.uid)
        if (savedUser != null) {
            user.id = savedUser.id
            db.userDao().update(mapper.modelToEntity(user))
        } else if (saveLocal) {
            user.id = db.userDao().add(mapper.modelToEntity(user))
        }
        if (user.photo != null) {
            val photoChanged = user.photo != savedUser?.photo
            if (photoChanged) downloadFile(getMiniPhotoFileName(user.photo), user.uid)
            if (loadFullPhoto) {
                val fileExist = fileExists(user.photo, context)
                if (photoChanged || !fileExist) downloadFile(user.photo, user.uid)
            }
        }
    }

    private suspend fun downloadFile(fileName: String, uid: String) =
        firebase.downloadFile(fileName, uid)

    private suspend fun uploadFile(fileName: String, uid: String) {
        val byteArray = getByteArray(false, fileName, context) ?: return
        firebase.uploadFile(fileName, uid, byteArray)
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}