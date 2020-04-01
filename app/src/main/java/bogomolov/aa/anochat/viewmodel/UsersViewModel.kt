package bogomolov.aa.anochat.viewmodel

import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import bogomolov.aa.anochat.android.UID
import bogomolov.aa.anochat.android.getSetting
import bogomolov.aa.anochat.android.isNotValidPhone
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import javax.inject.Inject

class UsersViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val searchLiveData = MutableLiveData<List<User>>()
    private val usersList = ArrayList<User>()

    fun loadContactUsers(contactPhones: List<String>): LiveData<PagedList<User>> {
        viewModelScope.launch(Dispatchers.IO) {
            val myUid = getSetting<String>(repository.getContext(), UID)
            val users = repository.receiveUsersByPhones(contactPhones).filter { it.uid != myUid }
            Log.i("test", "loadContactUsers finished")
            for (user in users) repository.updateUserFrom(user)
            usersList.clear()
            usersList.addAll(users)
        }
        return LivePagedListBuilder(repository.getUsersByPhones(contactPhones), 10).build()
    }

    fun getContacts(): List<String> {
        val phones = HashSet<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        repository.getContext().contentResolver.query(
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
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                phones += number.replace("[- ()]".toRegex(), "").replace("^8".toRegex(), "+7")
            }
        }
        return ArrayList(phones)
    }



    fun search(startWith: String) {
        if (isNotValidPhone(startWith)) {
            searchLiveData.value = usersList.filter { it.name.startsWith(startWith) }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val users = repository.findByPhone(startWith)
                for (user in users) repository.updateUserFrom(user, false)
                searchLiveData.postValue(users)
            }
        }
    }

    fun createConversation(user: User, onCreate: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversationId = repository.getConversation(user)
            withContext(Dispatchers.Main) {
                onCreate(conversationId)
            }
        }
    }
}