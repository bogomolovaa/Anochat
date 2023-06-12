package bogomolov.aa.anochat.domain.repositories

import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.User
import kotlinx.coroutines.flow.Flow

interface UserRepository : UserUseCasesInRepository {
    suspend fun getOrAddUser(uid: String, loadFullPhoto: Boolean = true): User
}

interface UserUseCasesInRepository {
    fun getImagesDataSource(userId: Long): Flow<PagingData<String>>
    suspend fun getUsersByPhones(phones: List<String>): List<User>

    suspend fun updateUsersByPhones(phones: List<String>): List<User>
    suspend fun updateUsersInConversations(blocking: Boolean)
    suspend fun getMyUser(): User
    suspend fun getUser(id: Long): User
    suspend fun updateMyUser(user: User)
    suspend fun searchByPhone(phone: String): List<User>
    suspend fun addUserStatusListener(uid: String): Flow<Triple<Boolean, Boolean, Long>>
}