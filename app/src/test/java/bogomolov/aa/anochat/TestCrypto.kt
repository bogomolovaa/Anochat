package bogomolov.aa.anochat

import android.util.Log
import bogomolov.aa.anochat.android.decrypt
import bogomolov.aa.anochat.android.encrypt
import bogomolov.aa.anochat.android.genKeyPair512
import bogomolov.aa.anochat.android.genSharedSecretKey
import org.junit.Test

import org.junit.Assert.*

class TestCrypto {
    @Test
    fun test() {

        //val baos = ByteArrayOutputStream()
        //bm.compress(Bitmap.CompressFormat.PNG, 100, baos) // bm is the bitmap object
        //val b: ByteArray = baos.toByteArray()

        /*
         KeyPair akp = genKeyPair512();
                KeyPair bkp = genKeyPair512();

            System.out.println("Ali pub key: "
                    + toRawHex(akp.getPublic().getEncoded()));
            System.out.println("Bob pub key: "
                    + toRawHex(bkp.getPublic().getEncoded()));

                System.out.println("Ali pri key: "
                    + toRawHex(akp.getPrivate().getEncoded()));
            System.out.println("Bob pri key: "
                    + toRawHex(bkp.getPrivate().getEncoded()));

            byte[] apk = akp.getPublic().getEncoded();
            byte[] bpk = bkp.getPublic().getEncoded();

            byte[] as = genSharedSecretKey(akp, bpk);
            byte[] bs = genSharedSecretKey(bkp, apk);
         */


        val keyPair1 = genKeyPair512()
        val publicKey1 = keyPair1!!.public
        val keyPair2 = genKeyPair512()
        val publicKey2 = keyPair2!!.public

        val secretKey = genSharedSecretKey(keyPair1.private.encoded, publicKey2.encoded)

        val encryptedData = encrypt(secretKey!!.encoded, "test string".toByteArray())
        val decryptedData = decrypt(secretKey.encoded, encryptedData!!)
        val decryptedString = String(decryptedData!!)
        println("decryptedData $decryptedString")
    }
}
