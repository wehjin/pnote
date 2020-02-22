package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.EditNote.Editing
import pnote.stories.EditNote.FinishedEditing
import pnote.stories.EditNoteAction.Cancel
import pnote.stories.EditNoteAction.Save
import pnote.tools.Note
import pnote.tools.Password
import pnote.tools.security.plain.PlainDocument

fun AppScope.editNoteStory(password: Password, noteId: Long): Story<EditNote> {
    val note = noteBag.readNote(password, noteId) as Note.Basic
    return matchingStory(
        name = "EditNote",
        toFirstVision = { Editing(note) },
        isLastVision = { it is FinishedEditing },
        updateRules = {
            onAction<Cancel, EditNote> { FinishedEditing }
            on<Save, EditNote, Editing> {
                val plainDoc = PlainDocument(
                    listOf(
                        action.title,
                        action.body
                    ).joinToString("\n").toCharArray()
                )
                val saveNote = Note.Basic(noteId, plainDoc)
                try {
                    noteBag.updateNote(password, saveNote)
                    FinishedEditing
                } catch (e: Throwable) {
                    vision.copy(saveError = e.toString())
                }
            }
        }
    )
}

sealed class EditNote {

    data class Editing(val note: Note, val saveError: String? = null) : EditNote() {
        fun cancel(): Any = Cancel
        fun save(title: String, body: String): Any = Save(title, body)
    }

    object FinishedEditing : EditNote()
}

private sealed class EditNoteAction {

    object Cancel : EditNoteAction()
    data class Save(val title: String, val body: String) : EditNoteAction()
}
