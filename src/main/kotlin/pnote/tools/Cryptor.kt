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

fun memCryptor(initSecret: String? = null, initPassword: String? = null): Cryptor = object : Cryptor {

    private var secret: String? = initSecret
    private var password: String? = initPassword

    override val accessLevel: AccessLevel
        get() = when {
            secret == null -> AccessLevel.Empty
            password != null && password == secret -> AccessLevel.ConfidentialUnlocked
            else -> AccessLevel.ConfidentialLocked
        }

    override fun importConfidential(password: String) {
        this.secret = password
    }

    override fun unlockConfidential(password: String) {
        this.password = password
    }
}