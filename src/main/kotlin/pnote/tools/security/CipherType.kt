package pnote.tools.security

sealed class CipherType {
    abstract val kdfSaltBytes: Int
    abstract val sfIvBytes: Int

    object Main : CipherType() {
        override val kdfSaltBytes: Int = 16
        override val sfIvBytes: Int = 16
        const val kdfLabel: String = "PBKDF2WithHmacSHA256"
        const val kdfRounds: Int = 200000
        const val sfLabel: String = "AES/GCM/NoPadding"
        const val sfKeyLabel: String = "AES"
        const val sfKeyBytes: Int = 32
        const val sfTagBytes: Int = 16
    }
}