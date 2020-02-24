package pnote.stories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pnote.scopes.AppScope
import pnote.tools.*
import pnote.tools.security.plain.PlainDocument
import story.core.scan


internal class EditNoteKtTest {

    private val password = password("a")
    private val initNoteId: Long = 1001
    private val initNote = Note.Basic(
        noteId = initNoteId,
        plainDoc = PlainDocument("Ho ho ho\nFull of sound and fury, signifying nothing".toCharArray())
    )
    private val app =
        object : AppScope {
            override val logTag: String = "edit-note-story-test"
            override val cryptor: Cryptor = memCryptor(password)
            override val noteBag: NoteBag = FileNoteBag(createTempDir(logTag), cryptor)
        }.apply {
            noteBag.updateNote(password, initNote)
        }

    private val story = app.editNoteStory(password, initNote.noteId)

    @Test
    internal fun `story saves note to note-bag`() {
        val newTitle = "Be bim bop"
        val newBody = "it is a nutritional meal"
        runBlocking {
            val editing = story.subscribe().receive() as EditNote.Editing
            story.offer(editing.save(newTitle, newBody))
            story.scan(1000) { it as? EditNote.FinishedEditing }
            val afterNote = app.noteBag.readNote(password, initNoteId) as Note.Basic
            assertEquals("$newTitle\n$newBody", afterNote.plainDoc.toCharSequence().toString())
        }
    }
}