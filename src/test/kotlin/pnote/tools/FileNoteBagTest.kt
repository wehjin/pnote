package pnote.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.tools.security.plain.PlainDocument

internal class FileNoteBagTest {

    private val dir = createTempDir("file-note-bag-test")
    private val secret = password("1234")
    private val cryptor = memCryptor().apply { unlockConfidential(secret) }
    private val bag = FileNoteBag(dir, cryptor)

    @Test
    internal fun `bag starts empty`() {
        val (accessLevel, banners) = bag.readNotes()
        accessLevel as AccessLevel.ConfidentialUnlocked
        assertEquals(0, banners.size)
    }

    @Test
    internal fun `bag adds and removes notes`() {
        val noteId = bag.createNote(secret, Note.Basic(plainDoc = PlainDocument("hello")))
        assertEquals(1, bag.readNotes().notes.size)

        bag.deleteNote(noteId, secret)
        assertEquals(0, bag.readNotes().notes.size)
    }

    @Test
    internal fun `bag updates notes`() {
        val noteId = bag.createNote(secret, Note.Basic(plainDoc = PlainDocument("hello")))
        val note = bag.readNote(secret, noteId) as Note.Basic
        bag.updateNote(secret, note.copy(plainDoc = PlainDocument("Goodbye")))
        val newNote = bag.readNote(secret, noteId) as Note.Basic
        assertEquals("Goodbye", newNote.plainDoc.toCharSequence().toString())
    }
}