package bogomolov.aa.anochat

import android.util.Log
import bogomolov.aa.anochat.android.*
import org.junit.Test

import org.junit.Assert.*

class TestCrypto {
    @Test
    fun test() {

        //val baos = ByteArrayOutputStream()
        //bm.compress(Bitmap.CompressFormat.PNG, 100, baos) // bm is the bitmap object
        //val b: ByteArray = baos.toByteArray()



        val keyPair1 = genKeyPair512()
        val publicKey1 = keyPair1!!.public
        val privateKeyByteArray = serializePrivateKey(keyPair1.private)

        val keyPair2 = genKeyPair512()
        val publicKey2 = keyPair2!!.public


        val privateKey1 = deserializePrivateKey(privateKeyByteArray)
        val secretKey = genSharedSecretKey(privateKey1, publicKey2.encoded)!!
        saveSecreteKey(secretKey, "secreteKey")
        val encryptedData = encrypt(secretKey!!.encoded, "test string".toByteArray())



        val loadedSecreteKey = getSecreteKey("secreteKey")
        val decryptedData = decrypt(loadedSecreteKey.encoded, encryptedData!!)
        val decryptedString = String(decryptedData!!)
        println("decryptedData $decryptedString")
    }
}
