package bogomolov.aa.anochat.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

private const val AES_KEY_SIZE = 128
private val IV = "12345678".toByteArray()

class Crypto @Inject constructor(private val keyValueStore: KeyValueStore) {

    fun getSecretKey(uid: String): SecretKey? {
        return getKey(getSecretKeyName(getMyUID()!!, uid))
    }

    fun encrypt(secretKey: SecretKey, clear: ByteArray): ByteArray {
        val raw = secretKey.encoded
        val skeySpec = SecretKeySpec(raw, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, IV)
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, spec)
        return cipher.doFinal(clear)
    }

    fun decrypt(secretKey: SecretKey, encrypted: ByteArray): ByteArray {
        try {
            val raw = secretKey.encoded
            val skeySpec = SecretKeySpec(raw, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, IV)
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, spec)
            return cipher.doFinal(encrypted)
        } catch (e: javax.crypto.AEADBadTagException) {
            throw WrongSecretKeyException("wrong")
        }
    }

    fun decryptString(string: String, secretKey: SecretKey): String {
        return String(decrypt(secretKey, keyValueStore.base64ToByteArray(string)))
    }

    fun encryptString(secretKey: SecretKey, string: String) =
        keyValueStore.byteArrayToBase64(encrypt(secretKey, string.toByteArray()))

    fun generateSecretKey(
        publicKeyString: String,
        uid: String
    ): SecretKey? {
        val myUid = getMyUID()!!
        val privateKey = getPrivateKey(myUid, uid)
        return if (privateKey != null) {
            val publicKeyByteArray = keyValueStore.base64ToByteArray(publicKeyString)
            val secretKey = genSharedSecretKey(privateKey, publicKeyByteArray)
            saveKey(getSecretKeyName(myUid, uid), secretKey)
            secretKey
        } else {
            null
        }
    }

    fun generatePublicKey(uid: String): String? {
        val keyPair = createKeyPair()
        val publicKeyByteArray = keyPair?.public?.encoded
        val privateKey = keyPair?.private
        return if (publicKeyByteArray != null && privateKey != null) {
            saveKey(getPrivateKeyName(getMyUID()!!, uid), privateKey)
            keyValueStore.byteArrayToBase64(publicKeyByteArray)
        } else null
    }


    private fun createKeyPair(): KeyPair? {
        try {
            //Generate params
            //val paramGen = AlgorithmParameterGenerator.getInstance("DH")
            //paramGen.init(512)
            //val params = paramGen.generateParameters().getParameterSpec(DHParameterSpec::class.java)
            //keyGen.initialize(params)

            //Use fixed
            val g = BigInteger(
                "7961C6D7913FDF8A034593294FA52D6F8354E9EDFE3EDC8EF082D36662D69DFE8CA7DC7480121C98B9774DFF915FB710D79E1BCBA68C0D429CD6B9AD73C0EF20",
                16
            )
            val p = BigInteger(
                "00AC86AB9A1F921B251027BD10B93D0A8D9A260364974648E2543E8CD5C48DB4FFBEF0C3843465BA8DE20FFA36FFAF840B8CF26C9EB865BA184642A5F84606AEC5",
                16
            )
            val keyGen = KeyPairGenerator.getInstance("DH")
            keyGen.initialize(DHParameterSpec(p, g, 511))
            return keyGen.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getPrivateKeyName(uid1: String, uid2: String) = "${uid1}${uid2}_private"

    private fun <T> saveKey(name: String, value: T) {
        val array = serializeKey(value)
        keyValueStore.setValue(name, array)
    }

    private fun <T> getKey(name: String): T? {
        val array: ByteArray? = keyValueStore.getValue(name)
        return if (array != null) deserializeKey<T>(array) else null
    }

    private fun getPrivateKey(myUid: String, uid: String): PrivateKey? =
        getKey(getPrivateKeyName(myUid, uid))

    private fun getSecretKeyName(uid1: String, uid2: String) = "${uid1}${uid2}_secret"

    private fun genSharedSecretKey(
        privateKey: PrivateKey,
        bytesPeerPublicKey: ByteArray
    ): SecretKey? {
        try {
            val x509KeySpecPublic = X509EncodedKeySpec(bytesPeerPublicKey)
            val peerPublicKey = KeyFactory.getInstance("DH").generatePublic(x509KeySpecPublic)

            val ka = KeyAgreement.getInstance("DH")
            ka.init(privateKey)
            ka.doPhase(peerPublicKey, true)
            val secret = ka.generateSecret()
            val sha256 = MessageDigest.getInstance("SHA-256")
            val bkey: ByteArray = Arrays.copyOf(
                sha256.digest(secret), AES_KEY_SIZE / java.lang.Byte.SIZE
            )
            return SecretKeySpec(bkey, "AES")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }
        return null
    }

    private fun <T> serializeKey(key: T): ByteArray {
        val b = ByteArrayOutputStream()
        val o = ObjectOutputStream(b)
        o.writeObject(key)
        val byteArray = b.toByteArray()
        o.close()
        b.close()
        return byteArray
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> deserializeKey(byteArray: ByteArray): T {
        val bi = ByteArrayInputStream(byteArray)
        val oi = ObjectInputStream(bi)
        val key = oi.readObject() as T
        oi.close()
        bi.close()
        return key
    }

    private fun getMyUID() = keyValueStore.getMyUID()
}

class WrongSecretKeyException(message: String) : Exception(message)