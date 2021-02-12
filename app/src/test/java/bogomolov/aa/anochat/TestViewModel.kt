package bogomolov.aa.anochat


import org.junit.Test

class TestViewModel {

    @Test
    fun testCreation(){
        /*
        val repository = object : Repository {
            override suspend fun updateUsersByPhones(phones: List<String>): List<User> {
                TODO("Not yet implemented")
            }

            override suspend fun updateUsersInConversations() {
                TODO("Not yet implemented")
            }

            override suspend fun getMyUser(): User {
                TODO("Not yet implemented")
            }

            override fun getUser(id: Long): User {
                TODO("Not yet implemented")
            }

            override suspend fun updateMyUser(user: User) {
                TODO("Not yet implemented")
            }

            override suspend fun searchByPhone(phone: String): List<User> {
                TODO("Not yet implemented")
            }

            override fun getConversation(id: Long): Conversation {
                TODO("Not yet implemented")
            }

            override suspend fun createConversation(user: User): Long {
                TODO("Not yet implemented")
            }

            override suspend fun deleteConversations(ids: Set<Long>) {
                TODO("Not yet implemented")
            }

            override suspend fun deleteConversationIfNoMessages(conversation: Conversation) {
                TODO("Not yet implemented")
            }

            override suspend fun receiveMessage(
                text: String,
                uid: String,
                messageId: String,
                replyId: String?,
                image: String?,
                audio: String?
            ): Message? {
                TODO("Not yet implemented")
            }

            override suspend fun sendMessage(message: Message, uid: String) {
                TODO("Not yet implemented")
            }

            override suspend fun deleteMessages(ids: Set<Long>) {
                TODO("Not yet implemented")
            }

            override fun updateSettings(settings: Settings) {
                TODO("Not yet implemented")
            }

            override fun getSettings(): Settings {
                TODO("Not yet implemented")
            }

            override fun generateSecretKey(publicKey: String, uid: String): Boolean {
                TODO("Not yet implemented")
            }

            override fun sendPublicKey(uid: String, initiator: Boolean) {
                TODO("Not yet implemented")
            }

            override suspend fun sendPendingMessages(uid: String) {
                TODO("Not yet implemented")
            }

            override fun receiveReport(messageId: String, received: Int, viewed: Int) {
                TODO("Not yet implemented")
            }

            override fun getImagesDataSource(userId: Long): DataSource.Factory<Int, String> {
                TODO("Not yet implemented")
            }

            override fun getUsersByPhonesDataSource(phones: List<String>): DataSource.Factory<Int, User> {
                TODO("Not yet implemented")
            }

            override fun searchMessagesDataSource(search: String): DataSource.Factory<Int, Conversation> {
                TODO("Not yet implemented")
            }

            override fun loadConversationsDataSource(): DataSource.Factory<Int, Conversation> {
                TODO("Not yet implemented")
            }

            override fun loadMessagesDataSource(
                conversationId: Long,
                scope: CoroutineScope
            ): DataSource.Factory<Int, Message> {
                TODO("Not yet implemented")
            }

            override suspend fun signUp(name: String, email: String, password: String): Boolean {
                TODO("Not yet implemented")
            }

            override suspend fun signIn(
                phoneNumber: String,
                credential: PhoneAuthCredential
            ): Boolean {
                TODO("Not yet implemented")
            }

            override fun signOut() {
                TODO("Not yet implemented")
            }

            override fun isSignedIn(): Boolean {
                TODO("Not yet implemented")
            }

            override fun addUserStatusListener(
                uid: String,
                scope: CoroutineScope
            ): Flow<Pair<Boolean, Long>> {
                TODO("Not yet implemented")
            }
        }
        val viewModel = UsersViewModel(repository)
        assert(viewModel!=null)
        */
    }
}