package pnote.tools

import java.io.File
import kotlin.properties.ReadWriteProperty

interface Cryptor {
    val accessLevel: AccessLevel
    fun importConfidential(secret: String)
    fun unlockConfidential(password: String)
}

enum class AccessLevel {
    Empty,
    ConfidentialLocked,
    ConfidentialUnlocked,
    Secret
}

fun fileCryptor(dir: File): Cryptor = secretPasswordCryptor(
    secretDelegate = FileStringDelegate(File(dir, "secret")),
    passwordDelegate = StringDelegate()
)

fun memCryptor(initSecret: String? = null, initPassword: String? = null): Cryptor = secretPasswordCryptor(
    secretDelegate = StringDelegate(initSecret),
    passwordDelegate = StringDelegate(initPassword)
)

fun secretPasswordCryptor(
    secretDelegate: ReadWriteProperty<Cryptor, String?>,
    passwordDelegate: ReadWriteProperty<Cryptor, String?>
) = object : Cryptor {
    private var secret: String? by secretDelegate
    private var password: String? by passwordDelegate

    override val accessLevel: AccessLevel
        get() = when {
            secret == null -> AccessLevel.Empty
            password != null && password == secret -> AccessLevel.ConfidentialUnlocked
            else -> AccessLevel.ConfidentialLocked
        }

    override fun importConfidential(secret: String) {
        this.secret = secret
        this.password = null
    }

    override fun unlockConfidential(password: String) {
        if (this.secret != null) {
            this.password = password
        }
    }
}
