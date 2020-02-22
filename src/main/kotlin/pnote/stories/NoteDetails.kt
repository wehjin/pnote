package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.offerWhenStoryEnds
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.NoteDetails.*
import pnote.stories.NoteDetailsAction.*
import pnote.tools.Note
import pnote.tools.Password
import pnote.tools.security.plain.PlainDocument


fun AppScope.noteDetailsStory(
    password: Password,
    noteId: Long,
    plainDoc: PlainDocument
): Story<NoteDetails> {
    return matchingStory(
        name = "NoteDetails",
        toFirstVision = { Viewing(plainDoc) },
        isLastVision = { it is FinishedViewing },
        updateRules = {

            onAction<Cancel, NoteDetails> {
                plainDoc.close()
                FinishedViewing
            }

            onAction<Reload, NoteDetails> {
                val note = noteBag.readNote(password, noteId) as Note.Basic
                Viewing(note.plainDoc)
            }

            on<Edit, NoteDetails, Viewing> {
                val substory = editNoteStory(password, noteId)
                offerWhenStoryEnds(substory) { Reload }
                Editing(substory)
            }
        }
    )
}

sealed class NoteDetails {
    data class Viewing(val plainDoc: PlainDocument) : NoteDetails() {
        fun edit(plainDoc: PlainDocument): Any = Edit(plainDoc)
    }

    data class Editing(val story: Story<EditNote>) : NoteDetails()

    object FinishedViewing : NoteDetails()

    val cancel: Any get() = Cancel

}

private sealed class NoteDetailsAction {
    object Cancel : NoteDetailsAction()
    data class Edit(val plainDoc: PlainDocument) : NoteDetailsAction()
    object Reload : NoteDetailsAction()
}
