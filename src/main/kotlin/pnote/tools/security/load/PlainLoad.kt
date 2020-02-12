package pnote.tools.security.load

class PlainLoad(
    val plainBytes: ByteArray,
    val cipherType: CipherType,
    val salt: ByteArray = randomSalt(cipherType.kdfSaltBytes),
    val iv: ByteArray = randomSalt(cipherType.sfIvBytes)
)