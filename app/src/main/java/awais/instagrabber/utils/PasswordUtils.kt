package awais.instagrabber.utils

import android.util.Base64
import java.security.GeneralSecurityException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object PasswordUtils {
    private const val cipherAlgo = "AES"
    private const val cipherTran = "AES/CBC/PKCS5Padding"
    @JvmStatic
    @Throws(Exception::class)
    fun dec(encrypted: String?, keyValue: ByteArray?): ByteArray {
        return try {
            val cipher = Cipher.getInstance(cipherTran)
            val secretKey = SecretKeySpec(keyValue, cipherAlgo)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
            cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT or Base64.NO_PADDING or Base64.NO_WRAP))
        } catch (e: NoSuchAlgorithmException) {
            throw IncorrectPasswordException(e)
        } catch (e: NoSuchPaddingException) {
            throw IncorrectPasswordException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw IncorrectPasswordException(e)
        } catch (e: InvalidKeyException) {
            throw IncorrectPasswordException(e)
        } catch (e: BadPaddingException) {
            throw IncorrectPasswordException(e)
        } catch (e: IllegalBlockSizeException) {
            throw IncorrectPasswordException(e)
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun enc(str: String, keyValue: ByteArray?): ByteArray {
        val cipher = Cipher.getInstance(cipherTran)
        val secretKey = SecretKeySpec(keyValue, cipherAlgo)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16)))
        val bytes = cipher.doFinal(str.toByteArray())
        return Base64.encode(bytes, Base64.DEFAULT or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    class IncorrectPasswordException(e: GeneralSecurityException?) : Exception(e)
}