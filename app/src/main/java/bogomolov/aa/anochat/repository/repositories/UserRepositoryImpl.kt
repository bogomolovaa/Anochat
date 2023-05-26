package bogomolov.aa.anochat.repository.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.entity.User
import bogomolov.aa.anochat.domain.getMyUID
import bogomolov.aa.anochat.domain.repositories.UserRepository
import bogomolov.aa.anochat.features.shared.getMiniPhotoFileName
import bogomolov.aa.anochat.repository.AppDatabase
import bogomolov.aa.anochat.repository.FileStore
import bogomolov.aa.anochat.repository.Firebase
import bogomolov.aa.anochat.repository.ModelEntityMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val firebase: Firebase,
    private val keyValueStore: KeyValueStore,
    private val fileStore: FileStore
) : UserRepository {
    private val mapper = ModelEntityMapper()

    override fun getImagesDataSource(userId: Long) =
        Pager(PagingConfig(pageSize = 10)) {
            db.messageDao().getImages(userId)
        }.flow.flowOn(Dispatchers.IO)

    override suspend fun getUsersByPhones(phones: List<String>) =
        withContext(Dispatchers.IO) {
            db.userDao().getAll(phones, getMyUID()!!).map { mapper.entityToModel(it)!! }
        }

    override suspend fun updateUsersByPhones(phones: List<String>) =
        withContext(Dispatchers.IO) {
            if (phones.isNotEmpty()) {
                val myUid = getMyUID()!!
                firebase.receiveUsersByPhones(phones).filter { it.uid != myUid }
                    .onEach { user -> updateLocalUserFromRemote(user, loadFullPhoto = false) }
            } else listOf()
        }

    override suspend fun updateUsersInConversations() {
        withContext(Dispatchers.IO) {
            val myUid = getMyUID() ?: return@withContext
            val users = db.userDao().getOpenedConversationUsers(myUid)
            mapper.entityToModel<User>(users).forEach { user ->
                firebase.getUser(user.uid)?.also { updateLocalUserFromRemote(it) }
            }
        }
    }

    override suspend fun getMyUser() = getOrAddUser(getMyUID()!!, false)

    override suspend fun getUser(id: Long) =
        withContext(Dispatchers.IO) {
            mapper.entityToModel(db.userDao().getUser(id))!!
        }

    override suspend fun updateMyUser(user: User) {
        withContext(Dispatchers.IO) {
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
    }

    override suspend fun searchByPhone(phone: String) =
        withContext(Dispatchers.IO) {
            firebase.findByPhone(phone).onEach { user ->
                updateLocalUserFromRemote(user, saveLocal = false, loadFullPhoto = false)
            }
        }

    override suspend fun addUserStatusListener(uid: String) =
        firebase.addUserStatusListener(getMyUID()!!, uid)

    override suspend fun getOrAddUser(uid: String, loadFullPhoto: Boolean): User =
        withContext(Dispatchers.IO) {
            val userEntity = db.userDao().findByUid(uid)
            val user = mapper.entityToModel(userEntity) ?: firebase.getUser(uid)!!
            user.also { updateLocalUserFromRemote(user = it, loadFullPhoto = loadFullPhoto) }
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
                val fileExist = fileStore.fileExists(user.photo)
                if (photoChanged || !fileExist) downloadFile(user.photo, user.uid)
            }
        }
    }

    private suspend fun downloadFile(fileName: String, uid: String) {
        val byteArray = firebase.downloadFile(fileName, uid)
        if (byteArray != null) fileStore.saveByteArray(byteArray, fileName, toGallery = false)
    }

    private suspend fun uploadFile(fileName: String, uid: String) {
        val byteArray = fileStore.getByteArray(false, fileName) ?: return
        firebase.uploadFile(fileName, uid, byteArray)
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}