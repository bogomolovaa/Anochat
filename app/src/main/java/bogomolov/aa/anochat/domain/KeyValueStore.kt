package bogomolov.aa.anochat.domain

private const val UID = "uid"

interface KeyValueStore {
    fun getByteArrayValue(key: String): ByteArray?
    fun getStringValue(key: String): String?
    fun getBooleanValue(key: String): Boolean
    fun setByteArrayValue(key: String, value: ByteArray)
    fun setStringValue(key: String, value: String?)
    fun setBooleanValue(key: String, value: Boolean)

    fun byteArrayToBase64(array: ByteArray): String
    fun base64ToByteArray(string: String): ByteArray
}

fun KeyValueStore.setMyUID(myUid: String?) = setValue(UID, myUid)

fun KeyValueStore.getMyUID() = getValue<String>(UID)

inline fun <reified T> KeyValueStore.setValue(key: String, value: T?) {
    when (T::class) {
        ByteArray::class -> setByteArrayValue(key, value as ByteArray)
        String::class -> setStringValue(key, value as String?)
        Boolean::class -> setBooleanValue(key, value as Boolean)
    }
}

inline fun <reified T> KeyValueStore.getValue(key: String) =
    when (T::class) {
        ByteArray::class -> getByteArrayValue(key) as T?
        String::class -> getStringValue(key) as T?
        Boolean::class -> getBooleanValue(key) as T
        else -> null
    }