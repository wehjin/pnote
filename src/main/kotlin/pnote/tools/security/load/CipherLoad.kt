package pnote.tools.security.load

class CipherLoad(
    val cipherBytes: ByteArray,
    val cipherType: CipherType,
    val salt: ByteArray,
    val iv: ByteArray
)