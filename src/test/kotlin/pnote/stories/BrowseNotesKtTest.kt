package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.Browsing
import pnote.stories.BrowseNotes.Finished
import pnote.tools.*
import story.core.scanVisions

internal class BrowseNotesKtTest {

    @Test
    internal fun happy() {
        val bannerSet = setOf(Banner.Basic(1, "Hello"))

        val appScope = object : AppScope {
            override val logTag: String = "${this.javaClass.simpleName}/happy"
            override val cryptor: Cryptor = memCryptor("1234")
            override val noteBag: NoteBag = object : NoteBag {
                override fun readBanners(): ReadBannersResult {
                    return ReadBannersResult.Banners(AccessLevel.ConfidentialLocked, bannerSet)
                }
            }
        }

        val story = appScope.browseNotes()
        runBlocking {
            val browsing = story.scanVisions(500) { it as? Browsing }
            assertEquals(AccessLevel.ConfidentialLocked, browsing.accessLevel)
            assertEquals(bannerSet, browsing.banners)
            browsing.cancel()
            story.scanVisions(500) { it as? Finished }
        }
    }
}