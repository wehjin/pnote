package pnote.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.tools.AccessLevel.ConfidentialLocked
import pnote.tools.AccessLevel.ConfidentialUnlocked

internal class CryptorKtTest {

    @Test
    internal fun `mem cryptor starts locked`() {
        val cryptor = memCryptor()
        assertEquals(ConfidentialLocked, cryptor.accessLevel)
    }

    @Test
    internal fun `mem cryptor unlocks confidential`() {
        val cryptor = memCryptor()
        cryptor.unlockConfidential(password("unlock"))
        val accessLevel = cryptor.accessLevel
        assertTrue(accessLevel is ConfidentialUnlocked)
    }
}