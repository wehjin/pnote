package pnote

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.ImportPassword.Action.Cancel
import pnote.ImportPassword.Action.SetPassword
import pnote.ImportPassword.UserError.InvalidPassword
import pnote.ImportPassword.UserError.MismatchedPasswords
import pnote.ImportPassword.Vision.FinishedGetPassword
import pnote.ImportPassword.Vision.GetPassword
import pnote.scopes.AppScope
import pnote.scopes.PasswordRef
import story.core.firstNotNull

class ImportPasswordTest : AppScope {
    override val logTag: String = "ImportPasswordTest"

    override fun importPassword(password: String): PasswordRef {
        return 137
    }

    private val story = importPasswordStory()

    @Test
    internal fun `story starts with empty fields`() {
        val vision = story.subscribe().poll()
        assertEquals(GetPassword("", "", null), vision)
    }

    @Test
    internal fun `story finishes with matching passwords`() {
        story.offer(SetPassword("hey", "hey"))
        val passwordRef = runBlocking {
            withTimeoutOrNull(500) {
                story.firstNotNull {
                    if (it is FinishedGetPassword) {
                        it.passwordRef
                    } else null
                }
            }
        }
        assertEquals(137, passwordRef)
    }

    @Test
    internal fun `story finishes after cancel`() {
        story.offer(Cancel)
        val hasPassword = runBlocking {
            withTimeoutOrNull(500) {
                story.firstNotNull {
                    if (it is FinishedGetPassword) {
                        it.passwordRef != null
                    } else null
                }
            }
        }
        assertEquals(false, hasPassword)
    }

    @Test
    internal fun `story errors with empty password`() {
        story.offer(SetPassword("", "hello"))
        val error = runBlocking {
            withTimeoutOrNull(500) {
                story.firstNotNull { vision ->
                    if (vision is GetPassword && vision.error != null) {
                        vision.error
                    } else {
                        null
                    }
                }
            }
        }
        assertEquals(InvalidPassword, error)
    }

    @Test
    internal fun `story errors with mismatched passwords`() {
        story.offer(SetPassword("hello", "Hello"))
        val error = runBlocking {
            withTimeoutOrNull(500) {
                story.firstNotNull { vision ->
                    if (vision is GetPassword && vision.error != null) {
                        vision.error
                    } else {
                        null
                    }
                }
            }
        }
        assertEquals(MismatchedPasswords, error)
    }
}