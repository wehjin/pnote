package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.tools.*
import pnote.tools.AccessLevel.*
import story.core.scan

internal class BrowseNotesKtTest {

    private val bannerSet = mutableSetOf(Banner.Basic(1, "Hello"))

    private val appScope = object : AppScope {
        override val logTag: String = "${this.javaClass.simpleName}/happy"

        override lateinit var cryptor: Cryptor

        override val noteBag: NoteBag = object : NoteBag {

            override fun addNote(password: Password, note: Note): Long {
                note as Note.Basic
                bannerSet.add(Banner.Basic(note.noteId, note.title))
                return note.noteId
            }

            override fun removeNote(noteId: Long, password: Password): Unit = error("not implemented")

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
        val story = appScope.browseNotesStory()
        runBlocking {
            val importing = story.scan(500) { it as? Importing }

            story.offer(importing.cancel())
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `locked cryptor starts story with unlocking`() {
        appScope.cryptor = memCryptor(password("1234"))
        val story = appScope.browseNotesStory()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }

            story.offer(unlocking.cancel())
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `unlocked cryptor starts story with browsing`() {
        appScope.cryptor = memCryptor(password("1234"), password("1234"))
        val story = appScope.browseNotesStory()
        runBlocking {
            val browsing = story.scan(500) { it as? Browsing }
            assertEquals(bannerSet, browsing.banners)

            story.offer(browsing.cancel())
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `offering an add-note action from browsing adds a note to the bag`() {
        appScope.cryptor = memCryptor(password("1234"), password("1234"))
        val story = appScope.browseNotesStory()
        runBlocking {
            val browsing = story.scan(500) { it as? Browsing }
            assertEquals(bannerSet, browsing.banners)

            story.offer(browsing.addNote("Adios"))
            val browsing2 = story.scan(500) { vision ->
                (vision as? Browsing)?.let { if (it.banners.size > 1) it else null }
            }
            val titles = browsing2.banners.map { (it as Banner.Basic).title }.toSet()
            assertTrue(titles.contains("Adios"))
        }
    }
}