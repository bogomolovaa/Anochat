package bogomolov.aa.anochat.android

import android.R.attr.password
import android.util.Log
import java.math.BigInteger
import java.security.*
import java.security.SecureRandom.getInstanceStrong
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


fun genKeyPair512(): KeyPair? {
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
    bytesPrivateKey: ByteArray,
    bytesPeerPublicKey: ByteArray
): SecretKey? {
    try {
        val x509KeySpecPrivate = X509EncodedKeySpec(bytesPrivateKey)
        val privateKey = KeyFactory.getInstance("DH").generatePublic(x509KeySpecPrivate)
        val x509KeySpecPublic = X509EncodedKeySpec(bytesPeerPublicKey)
        val peerPublicKey = KeyFactory.getInstance("DH").generatePublic(x509KeySpecPublic)

        val ka = KeyAgreement.getInstance("DH")
        ka.init(privateKey)
        ka.doPhase(peerPublicKey, true)
        return ka.generateSecret("AES")
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: InvalidKeyException) {
        e.printStackTrace()
    } catch (e: InvalidKeySpecException) {
        e.printStackTrace()
    }
    return null
}

fun encrypt(raw: ByteArray, clear: ByteArray): ByteArray? {
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    return cipher.doFinal(clear)
}

fun decrypt(raw: ByteArray, encrypted: ByteArray): ByteArray? {
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    return cipher.doFinal(encrypted)
}

fun encrypt2(raw: ByteArray, clear: ByteArray): ByteArray? {
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, cipher.getIV())
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, spec)
    return cipher.doFinal(clear)
}

fun decrypt2(raw: ByteArray, encrypted: ByteArray): ByteArray? {
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, cipher.iv)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec, spec)
    return cipher.doFinal(encrypted)
}

fun test1() {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)

    val spec = PBEKeySpec("password".toCharArray(), salt, 1000, 128 * 8)
    val secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec)

    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
}

