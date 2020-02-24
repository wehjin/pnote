package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes.*
import pnote.tools.*
import pnote.tools.AccessLevel.ConfidentialLocked
import pnote.tools.AccessLevel.ConfidentialUnlocked
import pnote.tools.security.plain.PlainDocument
import story.core.scan

internal class BrowseNotesKtTest {

    private val noteSet = mutableSetOf(Note.Basic(1, PlainDocument("Hello".toCharArray())))

    private val appScope = object : AppScope {
        override val logTag: String = "${this.javaClass.simpleName}/happy"

        override lateinit var cryptor: Cryptor

        override val noteBag: NoteBag = object : NoteBag {

            override fun createNote(password: Password, note: Note): Long {
                note as Note.Basic
                noteSet.add(Note.Basic(note.noteId, note.plainDoc))
                return note.noteId
            }

            override fun readNote(password: Password, noteId: Long): Note = error("not implemented")
            override fun updateNote(password: Password, note: Note) = error("not implemented")
            override fun deleteNote(noteId: Long, password: Password): Unit = error("not implemented")
            override fun readNotes(): ReadNotesResult = when (val accessLevel = cryptor.accessLevel) {
                ConfidentialLocked -> ReadNotesResult(accessLevel, emptySet())
                is ConfidentialUnlocked -> ReadNotesResult(accessLevel, noteSet)
            }
        }
    }

    @Test
    internal fun `locked cryptor starts story with unlocking`() {
        appScope.cryptor = memCryptor()
        val story = appScope.browseNotesStory()
        runBlocking {
            val unlocking = story.scan(500) { it as? Unlocking }

            unlocking.cancel()
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `unlocked cryptor starts story with browsing`() {
        appScope.cryptor = memCryptor(password("1234"))
        val story = appScope.browseNotesStory()
        runBlocking {
            val browsing = story.scan(500) { it as? Browsing }
            assertEquals(noteSet, browsing.notes)

            browsing.cancel()
            story.scan(500) { it as? Finished }
        }
    }

    @Test
    internal fun `adding a note from browsing adds a note to the bag`() {
        appScope.cryptor = memCryptor(password("1234"))
        val story = appScope.browseNotesStory()
        runBlocking {
            val browsing = story.scan(500) { it as? Browsing }
            assertEquals(noteSet, browsing.notes)

            browsing.addNote("Adios")
            val browsing2 = story.scan(500) { vision ->
                (vision as? Browsing)?.let { if (it.notes.size > 1) it else null }
            }
            val titles = browsing2.notes
                .map { it.plainDoc.toCharSequence().toString() }
                .toSet()
            assertTrue(titles.contains("Adios"))
        }
    }
}