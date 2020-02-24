package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.tools.AccessLevel
import pnote.tools.Cryptor
import pnote.tools.NoteBag
import pnote.tools.Password

internal class UnlockIdentityKtTest : AppScope {
    override val logTag: String = this.javaClass.simpleName

    override val noteBag: NoteBag
        get() = error("Not implemented")

    private var passwordResult: Password? = null
    override val cryptor: Cryptor = object : Cryptor {
        override val accessLevel: AccessLevel get() = error("not implemented")
        override fun unlockConfidential(password: Password) {
            passwordResult = password
        }
    }

    private val story = unlockIdentityStory()

    @Test
    internal fun `story begins with unlocking`() {
        runBlocking {
            story.subscribe().receive() is UnlockIdentity.Unlocking
        }
    }

    @Test
    internal fun `valid name moves story to done`() {
        val name = "joe-bob"
        val secret = charArrayOf('a', 'b')
        runBlocking {
            val importing = story.subscribe().receive() as UnlockIdentity.Unlocking
            importing.setSolName(name, secret).join()
            story.subscribe().receive() is UnlockIdentity.Done
            assertTrue(passwordResult!!.chars.contentEquals(secret))
        }
    }

    @Test
    internal fun `invalid name advances edition`() {
        val name = "joe*bob"
        val secret = charArrayOf('a', 'b')
        runBlocking {
            val importing = story.subscribe().receive() as UnlockIdentity.Unlocking
            importing.setSolName(name, secret).join()
            val stillImporting = story.subscribe().receive() as UnlockIdentity.Unlocking
            assertEquals(importing.edition + 1, stillImporting.edition)
        }
    }
}