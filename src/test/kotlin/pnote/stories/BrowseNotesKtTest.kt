package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.tools.*
import pnote.tools.AccessLevel.*
import story.core.scan

internal class BrowseNotesKtTest {

    private val bannerSet = setOf(Banner.Basic(1, "Hello"))
    private val appScope = object : AppScope {
        override val logTag: String = "${this.javaClass.simpleName}/happy"

        override lateinit var cryptor: Cryptor

        override val noteBag: NoteBag = object : NoteBag {
            override fun readBanners(): ReadBannersResult = when (val accessLevel = cryptor.accessLevel) {
                Empty -> ReadBannersResult(accessLevel, emptySet())
                ConfidentialLocked -> ReadBannersResult(accessLevel, emptySet())
                is ConfidentialUnlocked -> ReadBannersResult(accessLevel, bannerSet)
            }
        }
    }

    @Test
    internal fun `empty cryptor starts story with importing`() {
        appScope.cryptor = memCryptor(null)
        val story = appScope.browseNotes()
        runBlocking {
            val importing = story.scan(500) { it as? Importing }

            importing.cancel()
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `locked cryptor starts story with unlocking`() {
        appScope.cryptor = memCryptor(password("1234"))
        val story = appScope.browseNotes()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }

            unlocking.cancel()
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `unlocked cryptor starts story with browsing`() {
        appScope.cryptor = memCryptor(password("1234"), password("1234"))
        val story = appScope.browseNotes()
        runBlocking {
            val browsing = story.scan(500) { it as? Browsing }
            assertEquals(bannerSet, browsing.banners)

            browsing.cancel()
            story.scan(500) { it as? Finished }
        }
    }
}