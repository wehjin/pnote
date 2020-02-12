package pnote.tools.security

import java.io.Closeable
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface Flippable {
    val cipherType: CipherType
    val salt: ByteArray
    val iv: ByteArray
    val loadBytes: ByteArray
}

enum class FlipDirection {
    Encrypt,
    Decrypt;

    fun cipherMode(): Int = when (this) {
        Encrypt -> Cipher.ENCRYPT_MODE
        Decrypt -> Cipher.DECRYPT_MODE
    }
}

fun flip(flippable: Flippable, direction: FlipDirection, password: CharArray): ByteArray {
    val cipherType = flippable.cipherType as CipherType.Main
    return deriveSecret(password, flippable.salt, cipherType).use {
        val cipherKey = SecretKeySpec(it.key, CipherType.Main.sfKeyLabel)
        val cipherParams = GCMParameterSpec(CipherType.Main.sfTagBytes * 8, flippable.iv)
        val cipherMode = direction.cipherMode()
        val cipher = Cipher.getInstance(CipherType.Main.sfLabel)
        cipher.apply { init(cipherMode, cipherKey, cipherParams) }.doFinal(flippable.loadBytes)
    }
}

private fun deriveSecret(password: CharArray, salt: ByteArray, cipherType: CipherType): DerivedSecret {
    cipherType as CipherType.Main
    val key = PBEKeySpec(password, salt, CipherType.Main.kdfRounds, CipherType.Main.sfKeyBytes * 8)
        .let { SecretKeyFactory.getInstance(CipherType.Main.kdfLabel).generateSecret(it).encoded }
        .also { check(it.size == CipherType.Main.sfKeyBytes) }
    return DerivedSecret(key)
}

private class DerivedSecret(val key: ByteArray) : Closeable {

    override fun close() {
        for (i in key.indices) key[i] = 0
    }
}
