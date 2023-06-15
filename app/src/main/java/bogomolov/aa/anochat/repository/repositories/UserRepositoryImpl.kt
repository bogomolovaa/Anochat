package bogomolov.aa.anochat.repository.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import bogomolov.aa.anochat.features.shared.nameToImage
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    private val fileStore: FileStore,
    private val dispatcher: CoroutineDispatcher
) : UserRepository {
    private val mapper = ModelEntityMapper()

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun getImagesDataSource(userId: Long) =
        Pager(PagingConfig(pageSize = 10)) {
            db.messageDao().getImages(userId)
        }.flow.flowOn(dispatcher)

    override suspend fun getUsersByPhones(phones: List<String>) =
        withContext(dispatcher) {
            db.userDao().getAll(phones, getMyUID()).map { mapper.entityToModel(it)!! }
        }

    override suspend fun getAllUsers() =
        withContext(dispatcher) {
            db.userDao().getAll(getMyUID()).map { mapper.entityToModel(it)!! }
        }

    override suspend fun updateUsersByPhones(phones: List<String>) =
        withContext(dispatcher) {
            if (phones.isNotEmpty()) {
                val myUid = getMyUID()
                firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
                    .map { updateLocalUserFromRemote(it, loadFullPhoto = false) }
            } else listOf()
        }

    override suspend fun updateUsersInConversations(blocking: Boolean) {
        withContext(dispatcher) {
            val myUid = getMyUID()
            val users = db.userDao().getOpenedConversationUsers(myUid)
            mapper.entityToModel<User>(users).forEach { user ->
                firebase.getUser(user.uid)?.let { updateLocalUserFromRemote(it, blocking) }
            }
        }
    }

    override suspend fun getMyUser() = getOrAddUser(getMyUID(), false)

    override suspend fun getUser(id: Long) =
        withContext(dispatcher) {
            mapper.entityToModel(db.userDao().getUser(id))!!
        }

    override suspend fun updateMyUser(user: User) {
        withContext(dispatcher) {
            val savedUser = db.userDao().getUser(user.id)
            if (user.name != savedUser.name) firebase.renameUser(user.uid, user.name)
            if (user.status != savedUser.status) firebase.updateStatus(user.uid, user.status)
            if (user.photo != null && user.photo != savedUser.photo) {
                uploadFile(nameToImage(user.photo), user.uid)
                uploadFile(getMiniPhotoFileName(user.photo), user.uid)
                firebase.updatePhoto(user.uid, user.photo)
            }
            db.userDao().update(mapper.modelToEntity(user))
        }
    }

    override suspend fun searchByPhone(phone: String) =
        withContext(dispatcher) {
            firebase.findByPhone(phone).map { updateLocalUserFromRemote(it, saveLocal = false, loadFullPhoto = false) }
        }

    override suspend fun addUserStatusListener(uid: String) =
        firebase.addUserStatusListener(getMyUID(), uid)

    override suspend fun getOrAddUser(uid: String, loadFullPhoto: Boolean): User =
        withContext(dispatcher) {
            val userEntity = db.userDao().findByUid(uid)
            val user = mapper.entityToModel(userEntity) ?: firebase.getUser(uid)!!
            updateLocalUserFromRemote(user = user, loadFullPhoto = loadFullPhoto)
        }


    private suspend fun updateLocalUserFromRemote(
        user: User,
        saveLocal: Boolean = true,
        loadFullPhoto: Boolean = true,
        blocking: Boolean = false
    ): User {
        val savedUser = db.userDao().findByUid(user.uid)
        if (user.photo != null) {
            val photoChanged = user.photo != savedUser?.photo
            if (photoChanged) downloadFile(getMiniPhotoFileName(user.photo), user.uid, blocking)
            if (loadFullPhoto) {
                nameToImage(user.photo).let {
                    val fileExist = fileStore.fileExists(it)
                    if (photoChanged || !fileExist) downloadFile(it, user.uid, blocking)
                }
            }
        }
        return if (savedUser != null) {
            user.copy(id = savedUser.id).also { db.userDao().update(mapper.modelToEntity(it)) }
        } else if (saveLocal) {
            user.copy(id = db.userDao().add(mapper.modelToEntity(user)))
        } else user
    }

    private suspend fun downloadFile(fileName: String, uid: String, blocking: Boolean) {
        val update = suspend {
            val byteArray = firebase.downloadFile(fileName, uid)
            if (byteArray != null) fileStore.saveByteArray(byteArray, fileName, toGallery = false)
        }
        if (blocking) update()
        scope.launch { update() }
    }

    private fun uploadFile(fileName: String, uid: String) {
        scope.launch {
            val byteArray = fileStore.getByteArray(false, fileName) ?: return@launch
            firebase.uploadFile(fileName, uid, byteArray)
        }
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}