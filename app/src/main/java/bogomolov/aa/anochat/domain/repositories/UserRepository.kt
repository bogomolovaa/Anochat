package bogomolov.aa.anochat.domain.repositories

import androidx.paging.DataSource
import androidx.paging.PagingData
import bogomolov.aa.anochat.domain.entity.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface UserRepository : UserUseCasesInRepository {
    suspend fun getOrAddUser(uid: String): User
}

interface UserUseCasesInRepository {
    fun getImagesDataSource(userId: Long): Flow<PagingData<String>>
    fun getUsersByPhones(phones: List<String>): List<User>

    suspend fun updateUsersByPhones(phones: List<String>): List<User>
    suspend fun updateUsersInConversations()
    suspend fun getMyUser(): User
    fun getUser(id: Long): User
    suspend fun updateMyUser(user: User)
    suspend fun searchByPhone(phone: String): List<User>
    fun addUserStatusListener(
        uid: String,
        scope: CoroutineScope
    ): Flow<Triple<Boolean, Boolean, Long>>
}