package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.UnlockConfidential.Finished
import pnote.stories.UnlockConfidential.Unlocking
import pnote.tools.AccessLevel
import pnote.tools.Cryptor
import pnote.tools.NoteBag
import pnote.tools.memCryptor
import story.core.scan

internal class UnlockConfidentialKtTest {

    @Test
    internal fun `setting correct password changes access-level to unlocked`() {
        val logTag = "${this@UnlockConfidentialKtTest.javaClass.simpleName}/${"password"}"
        val confidentialPassword = "1234"

        val appScope = object : AppScope {
            override val noteBag: NoteBag get() = error("not implemented")
            override val cryptor: Cryptor = memCryptor("1234")
            override val logTag: String = logTag
        }
        val story = appScope.unlockConfidentialStory()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }
            unlocking.setPassword(confidentialPassword)
            story.scan(500) { it as? Finished }
            assertEquals(AccessLevel.ConfidentialUnlocked, appScope.cryptor.accessLevel)
        }
    }

    @Test
    internal fun `cancel produces finished vision`() {
        val logTag = "${this@UnlockConfidentialKtTest.javaClass.simpleName}/${"cancel"}"
        val appScope = object : AppScope {
            override val noteBag: NoteBag get() = error("not implemented")
            override val cryptor: Cryptor = memCryptor("1234")
            override val logTag: String = logTag
        }
        val story = appScope.unlockConfidentialStory()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }
            unlocking.cancel()
            story.scan(500) { it as? Finished }
        }
    }
}