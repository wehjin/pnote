package pnote.tools.security.load

import kotlin.random.Random

fun cipherLoad(password: CharArray, plainLoad: PlainLoad): CipherLoad {
    val flippable = plainLoad.asFlippable()
    val cipherBytes = flip(
        flippable,
        FlipDirection.Encrypt,
        password
    )
    return CipherLoad(
        cipherBytes,
        plainLoad.cipherType,
        plainLoad.salt,
        plainLoad.iv
    )
}

fun plainLoad(password: CharArray, cipherLoad: CipherLoad): PlainLoad? = try {
    val flippable = cipherLoad.asFlippable()
    val plainBytes = flip(
        flippable,
        FlipDirection.Decrypt,
        password
    )
    PlainLoad(
        plainBytes,
        cipherLoad.cipherType,
        cipherLoad.salt,
        cipherLoad.iv
    )
} catch (e: Throwable) {
    null
}

private fun PlainLoad.asFlippable(): Flippable {
    val plainLoad = this
    return object : Flippable {
        override val cipherType: CipherType get() = plainLoad.cipherType
        override val salt: ByteArray get() = plainLoad.salt
        override val iv: ByteArray get() = plainLoad.iv
        override val loadBytes: ByteArray get() = plainBytes
    }
}

private fun CipherLoad.asFlippable(): Flippable {
    val cipherLoad = this
    return object : Flippable {
        override val cipherType: CipherType get() = cipherLoad.cipherType
        override val salt: ByteArray get() = cipherLoad.salt
        override val iv: ByteArray get() = cipherLoad.iv
        override val loadBytes: ByteArray get() = cipherBytes
    }
}

fun randomSalt(bytes: Int) = ByteArray(bytes).also { Random.nextBytes(it) }

