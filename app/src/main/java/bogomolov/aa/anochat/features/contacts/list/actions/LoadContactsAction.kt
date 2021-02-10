package bogomolov.aa.anochat.features.contacts.list.actions

import android.content.Context
import android.provider.ContactsContract
import androidx.paging.LivePagedListBuilder
import bogomolov.aa.anochat.features.contacts.list.UsersActionContext
import bogomolov.aa.anochat.features.shared.UserAction
import bogomolov.aa.anochat.repository.UID
import bogomolov.aa.anochat.repository.getSetting
import bogomolov.aa.anochat.repository.isValidPhone
import java.util.ArrayList

class LoadContactsAction() : UserAction<UsersActionContext> {

    override suspend fun execute(context: UsersActionContext) {
        val phones = getContactPhones(context.repository.getContext())
        val pagedListLiveData =
            LivePagedListBuilder(context.repository.getUsersByPhones(phones), 10).build()
        context.viewModel.setState { copy(pagedListLiveData = pagedListLiveData) }
        syncUsers(context, phones)
        context.viewModel.setState { copy(synchronizationFinished = true) }
    }

    private suspend fun syncUsers(context: UsersActionContext, phones: List<String>) {
        val myUid = getSetting<String>(context.repository.getContext(), UID)
        val users = context.repository.receiveUsersByPhones(phones).filter { it.uid != myUid }
        for (user in users) context.repository.updateUserFrom(user)
        context.usersList = users
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
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
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