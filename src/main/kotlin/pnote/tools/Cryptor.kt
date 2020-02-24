package pnote.tools

import kotlin.properties.ReadWriteProperty

interface Cryptor {
    val accessLevel: AccessLevel
    fun unlockConfidential(password: Password)
}

sealed class AccessLevel {
    object ConfidentialLocked : AccessLevel()
    data class ConfidentialUnlocked(val password: Password) : AccessLevel()
}

fun memCryptor(initPassword: Password? = null): Cryptor =
    secretPasswordCryptor(passwordDelegate = PasswordDelegate(initPassword))

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
    passwordDelegate: ReadWriteProperty<Cryptor, Password?>
): Cryptor = object : Cryptor {
    private var password: Password? by passwordDelegate

    override val accessLevel: AccessLevel
        get() = when {
            password != null -> AccessLevel.ConfidentialUnlocked(password!!)
            else -> AccessLevel.ConfidentialLocked
        }

    override fun unlockConfidential(password: Password) {
        this.password = password
    }
}
