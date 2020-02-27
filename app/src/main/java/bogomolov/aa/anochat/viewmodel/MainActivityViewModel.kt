package bogomolov.aa.anochat.viewmodel

import androidx.lifecycle.ViewModel
import bogomolov.aa.anochat.repository.Repository
import javax.inject.Inject

class MainActivityViewModel
@Inject constructor(val repository: Repository) : ViewModel() {
    suspend fun isSignedIn() = repository.isSignedIn()
}