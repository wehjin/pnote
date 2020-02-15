package pnote.stories

import com.rubyhuntersky.story.core.Story
import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.NoteDetails.FinishedViewing
import pnote.stories.NoteDetails.Viewing


fun AppScope.noteDetailsStory(): Story<NoteDetails> = matchingStory<NoteDetails>(
    name = "NoteDetails",
    toFirstVision = { Viewing },
    isLastVision = { it is FinishedViewing },
    updateRules = {
        onAction<Action.Cancel, NoteDetails> { FinishedViewing }
    }
)

sealed class NoteDetails {
    val cancel get() = Action.Cancel as Any

    object Viewing : NoteDetails()
    object FinishedViewing : NoteDetails()
}

private sealed class Action {
    object Cancel : Action()
}