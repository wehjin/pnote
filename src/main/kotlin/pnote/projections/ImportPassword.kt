package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.ImportPassword

fun ProjectorScope.projectImportPassword(story: Story<ImportPassword.Vision>) = launch {
    loop@ for (vision in story.subscribe()) {
        when (vision) {
            is ImportPassword.Vision.GetPassword -> {
                screenLine()
                vision.passwordEntryError?.let { screenError("$it") }
                val passwordLine = promptLine("Enter password", "password")
                val checkLine = promptLine("Re-enter password", "password check")
                if (passwordLine.isNotEmpty() && checkLine.isNotEmpty()) {
                    vision.setPassword(passwordLine, checkLine)
                } else break@loop
            }
            is ImportPassword.Vision.FinishedGetPassword -> {
                screenLine("Got password")
                break@loop
            }
        }
    }
}
