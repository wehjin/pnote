package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.UnlockConfidential.Finished
import pnote.stories.UnlockConfidential.Unlocking
import pnote.tools.*
import story.core.scan

internal class UnlockConfidentialKtTest {

    @Test
    internal fun `setting correct password changes access-level to unlocked`() {
        val logTag = "${this@UnlockConfidentialKtTest.javaClass.simpleName}/${"password"}"
        val confidentialPassword = "1234"

        val appScope = object : AppScope {
            override val noteBag: NoteBag get() = error("not implemented")
            override val cryptor: Cryptor = memCryptor(password("1234"))
            override val logTag: String = logTag
        }
        val story = appScope.unlockConfidential()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }
            unlocking.setPassword(confidentialPassword)
            story.scan(500) { it as? Finished }
            assertTrue(appScope.cryptor.accessLevel is AccessLevel.ConfidentialUnlocked)
        }
    }

    @Test
    internal fun `cancel produces finished vision`() {
        val logTag = "${this@UnlockConfidentialKtTest.javaClass.simpleName}/${"cancel"}"
        val appScope = object : AppScope {
            override val noteBag: NoteBag get() = error("not implemented")
            override val cryptor: Cryptor = memCryptor(password("1234"))
            override val logTag: String = logTag
        }
        val story = appScope.unlockConfidential()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }
            unlocking.cancel()
            story.scan(500) { it as? Finished }
        }
    }
}