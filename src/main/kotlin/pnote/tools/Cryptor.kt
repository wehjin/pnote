package pnote.tools

interface Cryptor {
    val accessLevel: AccessLevel
    fun importConfidential(password: String)
    fun unlockConfidential(password: String)
}

enum class AccessLevel {
    Empty,
    ConfidentialLocked,
    ConfidentialUnlocked,
    Secret
}

fun memCryptor(storedSecret: String? = null): Cryptor = object : Cryptor {

    private var confidentialSecret: String? = storedSecret
    private var userSecret: String? = null

    override val accessLevel: AccessLevel
        get() = when {
            confidentialSecret == null -> AccessLevel.Empty
            userSecret != null && userSecret == confidentialSecret -> AccessLevel.ConfidentialUnlocked
            else -> AccessLevel.ConfidentialLocked
        }

    override fun importConfidential(password: String) {
        this.confidentialSecret = password
    }

    override fun unlockConfidential(password: String) {
        this.userSecret = password
    }
}