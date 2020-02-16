package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.projections.StringHandle
import pnote.scopes.AppScope
import pnote.stories.EdiNoteAction.Cancel
import pnote.stories.EditNote.Editing
import pnote.stories.EditNote.FinishedEditing

fun AppScope.editNoteStory(noteTitle: StringHandle): Story<EditNote> {
    return matchingStory(
        name = "EditNote",
        toFirstVision = { Editing(noteTitle) },
        isLastVision = { it is FinishedEditing },
        updateRules = { onAction<Cancel, EditNote> { FinishedEditing } }
    )
}

sealed class EditNote {

    data class Editing(val title: StringHandle) : EditNote() {
        fun cancel(): Any = Cancel
    }

    object FinishedEditing : EditNote()
}

private sealed class EdiNoteAction {

    object Cancel : EdiNoteAction()
}
