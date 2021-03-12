package bogomolov.aa.anochat

import bogomolov.aa.anochat.domain.Crypto
import bogomolov.aa.anochat.domain.WrongSecretKeyException
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.lang.Exception

private const val UID1 = "kjsdhfkasfdkjalshf_user1"
private const val UID2 = "hsdhfisfihkvneiuis_user2"

class TestCrypto {


    @Test
    fun test_encryptDecrypt() {
        val crypto1 = Crypto(MockKeyValueStore(UID1))
        val crypto2 = Crypto(MockKeyValueStore(UID2))
        val publicKey1 = crypto1.generatePublicKey(UID2)
        val publicKey2 = crypto2.generatePublicKey(UID1)
        val secretKey2 = crypto2.generateSecretKey(publicKey1!!, UID1)
        val secretKey1 = crypto1.generateSecretKey(publicKey2!!, UID2)


        val text = "hello world"
        val encrypted = crypto1.encryptString(secretKey1!!, text)
        val decrypted = crypto2.decryptString(encrypted, secretKey2!!)

        assertEquals(text, decrypted)
    }

    @Test
    fun test_encryptDecrypt2() {
        val uid1 = "kjsdhfkasfdkjalshf"
        val uid2 = "239849823749823743"
        val crypto1 = Crypto(MockKeyValueStore(uid1))
        val crypto2 = Crypto(MockKeyValueStore(uid2))
        val crypto11 = Crypto(MockKeyValueStore(uid1))
        val crypto22 = Crypto(MockKeyValueStore(uid2))
        val publicKey1 = crypto1.generatePublicKey(uid2)
        val publicKey2 = crypto2.generatePublicKey(uid1)
        val publicKey11 = crypto11.generatePublicKey(uid2)
        val publicKey22 = crypto22.generatePublicKey(uid1)
        val secretKey1 = crypto1.generateSecretKey(publicKey2!!, uid2)
        crypto11.generateSecretKey(publicKey22!!, uid2)
        crypto2.generateSecretKey(publicKey1!!, uid1)
        val secretKey22 = crypto22.generateSecretKey(publicKey11!!, uid1)


        val text = "hello world"
        val encrypted = crypto1.encryptString(secretKey1!!, text)
        var exception: Exception? = null
        try {
            val decrypted = crypto2.decryptString(encrypted, secretKey22!!)
        } catch (e: WrongSecretKeyException) {
            exception = e
        }
        assertThat("exception is WrongSecretKeyException",exception is WrongSecretKeyException)
    }
}