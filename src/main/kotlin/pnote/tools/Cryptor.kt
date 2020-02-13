package pnote.tools

import java.io.File
import kotlin.properties.ReadWriteProperty

interface Cryptor {
    val accessLevel: AccessLevel
    fun importConfidential(secret: Password)
    fun unlockConfidential(password: Password)
}

sealed class AccessLevel {
    object Empty : AccessLevel()
    object ConfidentialLocked : AccessLevel()
    data class ConfidentialUnlocked(val password: Password) : AccessLevel()
}

fun fileCryptor(dir: File): Cryptor = secretPasswordCryptor(
    secretDelegate = FilePasswordDelegate(File(dir, "secret")),
    passwordDelegate = PasswordDelegate()
)

fun memCryptor(
    initSecret: Password? = null,
    initPassword: Password? = null
): Cryptor = secretPasswordCryptor(
    secretDelegate = PasswordDelegate(initSecret),
    passwordDelegate = PasswordDelegate(initPassword)
)

data class Password(val chars: CharArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Password
        if (!chars.contentEquals(other.chars)) return false
        return true
    }

    override fun hashCode(): Int = chars.contentHashCode()
}

fun password(string: String): Password = Password(string.toCharArray())

fun secretPasswordCryptor(
    secretDelegate: ReadWriteProperty<Cryptor, Password?>,
    passwordDelegate: ReadWriteProperty<Cryptor, Password?>
) = object : Cryptor {
    private var secret: Password? by secretDelegate
    private var password: Password? by passwordDelegate

    override val accessLevel: AccessLevel
        get() = when {
            secret == null -> AccessLevel.Empty
            (password != null && password == secret) -> AccessLevel.ConfidentialUnlocked(password!!)
            else -> AccessLevel.ConfidentialLocked
        }

    override fun importConfidential(secret: Password) {
        this.secret = secret
        this.password = null
    }

    override fun unlockConfidential(password: Password) {
        if (this.secret != null) {
            this.password = password
        }
    }
}
