package bogomolov.aa.anochat.viewmodel

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bogomolov.aa.anochat.core.User
import bogomolov.aa.anochat.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UsersViewModel
@Inject constructor(private val repository: Repository) : ViewModel() {
    val usersLiveData = MutableLiveData<List<User>>()

    fun loadContactUsers(){
        val contactPhones = getContacts()
        
    }

    private fun getContacts(): List<String> {
        val phones = ArrayList<String>()
        val PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cr = repository.getContext().contentResolver
        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PROJECTION,
            null,
            null,
            null
        )
        if (cursor != null) {
            try {
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    //val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)
                    phones+=number
                }
            } finally {
                cursor.close()
            }
        }
        return phones
    }

    fun search(startWith: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = repository.findUsers(startWith)
            for (user in users) repository.updateUserFrom(user)
            usersLiveData.postValue(users)
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