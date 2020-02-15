package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.projections.StringHandle
import pnote.scopes.AppScope
import pnote.stories.NoteDetails.FinishedViewing
import pnote.stories.NoteDetails.Viewing
import pnote.tools.Password


fun AppScope.noteDetailsStory(title: StringHandle, password: Password, noteId: Long): Story<NoteDetails> {
    return matchingStory(
        name = "NoteDetails",
        toFirstVision = { Viewing(title) },
        isLastVision = { it is FinishedViewing },
        updateRules = { onAction<Action.Cancel, NoteDetails> { FinishedViewing } }
    )
}

sealed class NoteDetails {
    val cancel get() = Action.Cancel as Any

    data class Viewing(val title: StringHandle) : NoteDetails()
    object FinishedViewing : NoteDetails()
}

private sealed class Action {
    object Cancel : Action()
}