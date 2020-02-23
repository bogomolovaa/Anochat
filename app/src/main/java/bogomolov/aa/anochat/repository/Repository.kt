package bogomolov.aa.anochat.repository

import androidx.paging.DataSource
import androidx.paging.PagedList
import bogomolov.aa.anochat.core.Message

interface Repository {
    fun addMessage(message: Message)

    fun loadMessages(conversationId: Long): DataSource.Factory<Int, Message>

}