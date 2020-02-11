package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.ImportPasswordVision.FinishedGetPassword
import pnote.stories.ImportPasswordVision.GetPassword
import pnote.stories.PasswordEntryError.InvalidPassword
import pnote.stories.PasswordEntryError.MismatchedPasswords
import pnote.tools.AccessLevel
import pnote.tools.Cryptor
import pnote.tools.NoteBag
import pnote.tools.memCryptor
import story.core.scan

class ImportPasswordTest : AppScope {
    override val logTag: String = "ImportPasswordTest"
    override val cryptor: Cryptor = memCryptor()
    override val noteBag: NoteBag get() = error("not implemented")

    private val story = importPassword()

    @Test
    internal fun `story starts with empty fields`() {
        runBlocking {
            val getPassword = story.scan(300) { it as? GetPassword }
            assertEquals("", getPassword.password)
            assertEquals("", getPassword.check)
            assertEquals(null, getPassword.passwordEntryError)
        }
    }

    @Test
    internal fun `story finishes after cancel`() {
        runBlocking {
            val getPassword = story.scan(300) { it as? GetPassword }
            getPassword.cancel()

            story.scan(300) { it as? FinishedGetPassword }
            assertEquals(AccessLevel.Empty, cryptor.accessLevel)
        }
    }

    @Test
    internal fun `story finishes with matching passwords`() {
        runBlocking {
            val getPassword = story.scan(300) { it as? GetPassword }
            getPassword.setPassword("hey", "hey")
            story.scan(300) { it as? FinishedGetPassword }
            assertEquals(AccessLevel.ConfidentialLocked, cryptor.accessLevel)
        }
    }

    @Test
    internal fun `story errors with empty password`() {
        runBlocking {
            val getPassword = story.scan(300) { it as? GetPassword }
            getPassword.setPassword("", "hello")
            val entryError = story.scan(300) { (it as? GetPassword)?.passwordEntryError }
            assertEquals(InvalidPassword, entryError)
        }
    }

    @Test
    internal fun `story errors with mismatched passwords`() {
        runBlocking {
            val getPassword = story.scan(300) { it as? GetPassword }
            getPassword.setPassword("hello", "Hello")
            val entryError = story.scan(300) { (it as? GetPassword)?.passwordEntryError }
            assertEquals(MismatchedPasswords, entryError)
        }
    }
}