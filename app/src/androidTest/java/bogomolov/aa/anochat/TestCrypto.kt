package bogomolov.aa.anochat

import android.util.Log
import bogomolov.aa.anochat.android.*
import org.junit.Test

import org.junit.Assert.*
import java.security.PrivateKey
import javax.crypto.SecretKey

class TestCrypto {
    @Test
    fun test_encryptDecrypt() {
        val testString = "test string"
        val keyPair1 = createKeyPair()
        val publicKey1 = keyPair1!!.public
        val privateKeyByteArray = serializeKey(keyPair1.private)

        val keyPair2 = createKeyPair()
        val publicKey2 = keyPair2!!.public


        val privateKey1 = deserializeKey<PrivateKey>(privateKeyByteArray)
        val secretKey = genSharedSecretKey(privateKey1, publicKey2.encoded)!!
        val secretKeyArray = serializeKey(secretKey)
        val encryptedData = encrypt(secretKey, testString.toByteArray())


        val loadedSecreteKey = deserializeKey<SecretKey>(secretKeyArray)
        val decryptedData = decrypt(loadedSecreteKey, encryptedData)
        val decryptedString = String(decryptedData)

        assertEquals(testString,decryptedString)

    }
}
