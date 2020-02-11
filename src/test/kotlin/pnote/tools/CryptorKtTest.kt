package pnote.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class CryptorKtTest {

    @Test
    internal fun `file cryptor starts empty`() {
        val cryptor = fileCryptor(createTempDir())
        assertEquals(AccessLevel.Empty, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor stays empty after unlock`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.unlockConfidential("empty-unlock")
        assertEquals(AccessLevel.Empty, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor turns locked after import`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential("import")
        assertEquals(AccessLevel.ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file crypt stays locked after false unlock`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential("import-and-unlock")
        cryptor.unlockConfidential("import-and-fail-unlock")
        assertEquals(AccessLevel.ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor imports and unlocks confidential`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential("import-and-unlock")
        cryptor.unlockConfidential("import-and-unlock")
        assertEquals(AccessLevel.ConfidentialUnlocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor turns locked after re-import`() {
        val cryptor = fileCryptor(createTempDir())
        cryptor.importConfidential("import-and-unlock")
        cryptor.unlockConfidential("import-and-unlock")
        cryptor.importConfidential("re-import")
        assertEquals(AccessLevel.ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `file cryptor retains secret and discards password after reload`() {
        val dir = createTempDir()
        fileCryptor(dir).apply {
            importConfidential("import-and-unlock")
            unlockConfidential("import-and-unlock")
        }

        val reload = fileCryptor(dir)
        assertEquals(AccessLevel.ConfidentialLocked, reload.accessLevel)
    }
}