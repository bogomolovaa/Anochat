package bogomolov.aa.anochat.viewmodel

import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.android.UID
import bogomolov.aa.anochat.android.getSetting
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
import javax.inject.Inject

class UsersViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val usersLiveData = MutableLiveData<List<User>>()
    private var contactUsers: ArrayList<User>? = null

    fun loadContactUsers() {
        if (contactUsers == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val contactPhones = getContacts()
                for (phone in contactPhones) Log.i("test", phone)
                val myUid = getSetting<String>(repository.getContext(), UID)
                val users =
                    repository.getUsersByPhones(
                        listOf(
                            "+79057148736",
                            "+79689292630",
                            "+79031132612"
                        )
                    ).filter { it.uid != myUid }

                Log.i("test", "loadContactUsers finished")
                for (user in users) repository.updateUserFrom(user)
                usersLiveData.postValue(users)
                contactUsers = ArrayList()
                contactUsers!!.addAll(users)
            }
        } else {
            usersLiveData.postValue(contactUsers)
        }
    }

    private fun getContacts(): List<String> {
        val phones = HashSet<String>()
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        repository.getContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION,
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
                Log.i("test", "contact $name $number")
            }
        }
        return ArrayList(phones)
    }

    private fun isNotValidPhone(string: String) = string.contains("[^+0-9]".toRegex())

    fun search(startWith: String) {
        if (isNotValidPhone(startWith)) {
            usersLiveData.value = contactUsers?.filter { it.name.startsWith(startWith) } ?: listOf()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val users = repository.findByPhone(startWith)
                for (user in users) repository.updateUserFrom(user)
                usersLiveData.postValue(users)
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