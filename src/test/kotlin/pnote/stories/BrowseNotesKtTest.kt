package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.AccessLevel
import pnote.scopes.AppScope
import pnote.scopes.PasswordRef
import pnote.tools.Banner
import pnote.tools.NoteBag
import pnote.tools.ReadBannersResult

internal class BrowseNotesKtTest {

    @Test
    internal fun `vision contains banners when cryptor is unlocked`() {
        val bannerSet = setOf(Banner.Basic(1, "Hello"))
        val appScope = object : AppScope {
            override val logTag: String = "BrowseNotesKtTest/unlocked"

            override val noteBag: NoteBag = object : NoteBag {
                override fun readBanners(): ReadBannersResult {
                    return ReadBannersResult.Banners(AccessLevel.Confidential, bannerSet)
                }
            }

            override fun importPassword(password: String): PasswordRef {
                error("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        val story = appScope.browseNotes()
        val vision = runBlocking { story.subscribe().receive() }
        vision as BrowsingNotes
        assertEquals(bannerSet, vision.banners)
    }
}