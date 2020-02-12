package pnote.tools.security

class CipherLoad(
    val cipherBytes: ByteArray,
    val cipherType: CipherType,
    val salt: ByteArray,
    val iv: ByteArray
)