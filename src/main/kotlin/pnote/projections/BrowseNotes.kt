package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.BrowseNotes
import pnote.stories.BrowseNotes.*
import pnote.stories.UnlockConfidential
import pnote.tools.Banner

fun ProjectorScope.projectBrowseNotes(story: Story<BrowseNotes>) = launch {
    visionLoop@ for (vision in story.subscribe()) {
        when (vision) {
            is Importing -> projectImportPassword(vision.substory)
            is Unlocking -> projectUnlockConfidential(vision.substory)
            is Browsing -> {
                screenLine()
                vision.banners.forEachIndexed { i, banner ->
                    banner as Banner.Basic
                    val message = banner.title
                    screenLine("${i + 1}:\u2002$message")
                }
                commandLoop@ for (command in generateSequence { promptLine("CONFIDENTIAL", "command") }) {
                    val done = when (command) {
                        "", "done", "quit", "exit", "lock", "bye" -> true.also {
                            vision.cancel()
                        }
                        else -> false.also { screenError("Hmm. I fail to understand the significance of this command '$command'") }
                    }
                    if (done) break@commandLoop
                }
            }
            Finished -> break@visionLoop
        }
    }
}

fun ProjectorScope.projectUnlockConfidential(story: Story<UnlockConfidential>) = launch {
    visionLoop@ for (vision in story.subscribe()) {
        when (vision) {
            is UnlockConfidential.Unlocking -> {
                screenLine()
                if (vision.invalidAttempt != null) {
                    screenError("Invalid password")
                }
                val passwordLine = promptLine("Enter password", "unlock-password")
                if (passwordLine.isBlank()) {
                    vision.cancel()
                } else {
                    vision.setPassword(passwordLine)
                }
            }
            is UnlockConfidential.Finished -> break@visionLoop
        }
    }
}
