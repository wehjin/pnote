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
    internal fun happy() {
        val bannerSet = setOf(Banner.Basic(1, "Hello"))
        val appScope = object : AppScope {
            override val logTag: String = "${this.javaClass.simpleName}/happy"

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
        runBlocking {
            val browsing = story.subscribe().receive() as BrowsingNotes
            assertEquals(bannerSet, browsing.banners)

            browsing.cancel()
            story.subscribe().receive() as FinishedBrowsingNotes
        }
    }
}