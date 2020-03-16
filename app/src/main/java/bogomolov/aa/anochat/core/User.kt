package bogomolov.aa.anochat.core

data class User(var id: Long = 0, val uid: String, val phone: String ,var name: String, var photo: String? = null, var status: String? = null)
