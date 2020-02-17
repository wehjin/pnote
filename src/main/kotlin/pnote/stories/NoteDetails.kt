package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.offerWhenStoryEnds
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.projections.StringHandle
import pnote.scopes.AppScope
import pnote.stories.NoteDetails.*
import pnote.stories.NoteDetailsAction.*
import pnote.tools.Note
import pnote.tools.Password


fun AppScope.noteDetailsStory(password: Password, noteId: Long, title: StringHandle): Story<NoteDetails> {
    return matchingStory(
        name = "NoteDetails",
        toFirstVision = { Viewing(title) },
        isLastVision = { it is FinishedViewing },
        updateRules = {
            onAction<Cancel, NoteDetails> { FinishedViewing }

            onAction<Reload, NoteDetails> {
                val note = noteBag.readNote(password, noteId) as Note.Basic
                Viewing(note.title)
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
    data class Viewing(val title: StringHandle) : NoteDetails() {
        fun edit(title: StringHandle): Any = Edit(title)
    }

    data class Editing(val story: Story<EditNote>) : NoteDetails()

    object FinishedViewing : NoteDetails()

    val cancel: Any get() = Cancel

}

private sealed class NoteDetailsAction {
    object Cancel : NoteDetailsAction()
    data class Edit(val title: StringHandle) : NoteDetailsAction()
    object Reload : NoteDetailsAction()
}
