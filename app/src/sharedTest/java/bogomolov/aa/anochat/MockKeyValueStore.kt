package bogomolov.aa.anochat

import bogomolov.aa.anochat.domain.KeyValueStore
import bogomolov.aa.anochat.domain.UID

class MockKeyValueStore(myUid: String) : KeyValueStore {
    private val stringMap = HashMap<String, String?>()
    private val bytesMap = HashMap<String, ByteArray>()
    private val booleanMap = HashMap<String, Boolean?>()

    init {
        stringMap[UID] = myUid
    }

    override fun getByteArrayValue(key: String): ByteArray? {
        return bytesMap[key]
    }

    override fun getStringValue(key: String) = stringMap[key]

    override fun getBooleanValue(key: String) = booleanMap[key] ?: false

    override fun setByteArrayValue(key: String, value: ByteArray) {
        bytesMap[key] = value
    }

    override fun setStringValue(key: String, value: String?) {
        stringMap[key] = value
    }

    override fun setBooleanValue(key: String, value: Boolean) {
        booleanMap[key] = value
    }

    override fun byteArrayToBase64(array: ByteArray): String =
        java.util.Base64.getEncoder().encodeToString(array)

    override fun base64ToByteArray(string: String): ByteArray =
        java.util.Base64.getDecoder().decode(string)
}