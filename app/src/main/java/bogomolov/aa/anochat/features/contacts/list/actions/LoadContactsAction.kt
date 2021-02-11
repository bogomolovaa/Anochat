package bogomolov.aa.anochat.features.contacts.list.actions

import android.content.Context
import android.provider.ContactsContract
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.isValidPhone
import java.util.*
import kotlin.collections.HashSet

class LoadContactsAction() : UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        val phones = getContactPhones(context.repository.getContext())
        val pagedListLiveData =
            LivePagedListBuilder(context.repository.getUsersByPhonesDataSource(phones), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        context.usersList = context.repository.updateUsersByPhones(phones)
        context.viewModel.setState { copy(synchronizationFinished = true) }
    }

    private fun getContactPhones(context: Context): List<String> {
        val phones = HashSet<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            val numberIndex =
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val clearNumber =
                    number.replace("[- ()]".toRegex(), "").replace("^8".toRegex(), "+7")
                if (isValidPhone(clearNumber)) phones += clearNumber
            }
        }
        return ArrayList(phones)
    }
}