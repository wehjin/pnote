package pnote.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.tools.AccessLevel.*

internal class CryptorKtTest {

    @Test
    internal fun `file cryptor starts empty`() {
        val cryptor = fileCryptor(createTempDir())
        assertEquals(Empty, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor stays empty after unlock`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.unlockConfidential(password("empty-unlock"))
        assertEquals(Empty, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor turns locked after import`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential(password("import"))
        assertEquals(ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file crypt stays locked after false unlock`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential(password("import-and-unlock"))
        cryptor.unlockConfidential(password("import-and-fail-unlock"))
        assertEquals(ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor imports and unlocks confidential`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential(password("import-and-unlock"))
        cryptor.unlockConfidential(password("import-and-unlock"))
        val accessLevel = cryptor.accessLevel
        assertTrue(accessLevel is ConfidentialUnlocked)
    }

    @Test
    internal fun `file cryptor turns locked after re-import`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential(password("import-and-unlock"))
        cryptor.unlockConfidential(password("import-and-unlock"))
        cryptor.importConfidential(password("re-import"))
        assertEquals(ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor retains secret and discards password after reload`() {
        val dir = createTempDir()
        fileCryptor(dir).apply {
            importConfidential(password("import-and-unlock"))
            unlockConfidential(password("import-and-unlock"))
        }

        val reload = fileCryptor(dir)
        assertEquals(ConfidentialLocked, reload.accessLevel)
    }
}