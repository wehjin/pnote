package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.ImportPasswordVision
import pnote.stories.ImportPasswordVision.FinishedGetPassword
import pnote.stories.ImportPasswordVision.GetPassword

fun ProjectorScope.projectImportPassword(story: Story<ImportPasswordVision>) = launch {
    loop@ for (vision in story.subscribe()) {
        when (vision) {
            is GetPassword -> {
                screenLine()
                vision.passwordEntryError?.let { screenError("$it") }
                val passwordLine = promptLine("Enter password", "password")
                val checkLine = promptLine("Re-enter password", "password check")
                if (passwordLine.isNotEmpty() && checkLine.isNotEmpty()) {
                    vision.setPassword(passwordLine, checkLine)
                } else {
                    vision.cancel()
                }
            }
            is FinishedGetPassword -> break@loop
        }
    }
}
