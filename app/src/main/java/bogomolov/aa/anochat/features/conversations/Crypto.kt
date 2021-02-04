package bogomolov.aa.anochat.features.conversations

import android.content.Context
import android.util.Base64.DEFAULT
import android.util.Log
import bogomolov.aa.anochat.repository.UID
import bogomolov.aa.anochat.repository.getSetting
import bogomolov.aa.anochat.repository.setSetting
import java.io.*
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

private const val AES_KEY_SIZE = 128
private val IV = "12345678".toByteArray()


fun createKeyPair(): KeyPair? {
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


fun genSharedSecretKey(
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
        val desSpec: SecretKey = SecretKeySpec(bkey, "AES")
        return desSpec
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: InvalidKeyException) {
        e.printStackTrace()
    } catch (e: InvalidKeySpecException) {
        e.printStackTrace()
    }
    return null
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
    val raw = secretKey.encoded
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, IV)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, spec)
    return cipher.doFinal(encrypted)
}

fun <T> serializeKey(key: T): ByteArray {
    val b = ByteArrayOutputStream()
    val o = ObjectOutputStream(b)
    o.writeObject(key)
    val byteArray = b.toByteArray()
    o.close()
    b.close()
    return byteArray
}

fun <T> deserializeKey(byteArray: ByteArray): T {
    val bi = ByteArrayInputStream(byteArray)
    val oi = ObjectInputStream(bi)
    val key = oi.readObject() as T
    oi.close()
    bi.close()
    return key
}

fun getSecretKeyName(uid1: String, uid2: String) = "${uid1}${uid2}_secret"

fun getPrivateKeyName(uid1: String, uid2: String) = "${uid1}${uid2}_private"

fun <T> saveKey(name: String, value: T, context: Context) {
    val array = serializeKey(value)
    setSetting(context, name, array)
}

fun <T> getKey(name: String, context: Context): T? {
    val array = getSetting<ByteArray>(context, name)
    return if (array != null) deserializeKey<T>(array) else null
}

fun getSecretKey(myUid: String, uid: String, context: Context) =
    getKey<SecretKey>(getSecretKeyName(myUid, uid), context)

fun getPrivateKey(myUid: String, uid: String, context: Context) =
    getKey<PrivateKey>(getPrivateKeyName(myUid, uid), context)

fun byteArrayToBase64(array: ByteArray) = android.util.Base64.encodeToString(array, DEFAULT)

fun base64ToByteArray(string: String) = android.util.Base64.decode(string, DEFAULT)

fun decryptFile(file: File, uid: String, context: Context) {
    val myUid = getSetting<String>(context, UID)!!
    val secretKey = getSecretKey(myUid, uid, context)
    if (secretKey != null) {
        val decryptedByteArray = decrypt(secretKey, file.readBytes())
        file.writeBytes(decryptedByteArray)
    }
}

fun encryptFile(file: File, uid: String, context: Context): ByteArray? {
    val myUid = getSetting<String>(context, UID)!!
    val secretKey = getSecretKey(myUid, uid, context)
    return if (secretKey != null) encrypt(secretKey, file.readBytes()) else null
}

fun decryptString(secretKey: SecretKey, string: String) =
    String(decrypt(secretKey, base64ToByteArray(string)))

fun encryptString(secretKey: SecretKey, string: String) =
    byteArrayToBase64(encrypt(secretKey, string.toByteArray()))

fun generateAndSaveSecretKey(
    privateKey: PrivateKey,
    publicKeyString: String,
    myUid: String,
    uid: String,
    context: Context
) {
    val publicKeyByteArray = base64ToByteArray(publicKeyString)
    val secretKey = genSharedSecretKey(privateKey, publicKeyByteArray)
    saveKey(getSecretKeyName(myUid, uid), secretKey, context)
}
