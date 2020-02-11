package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.BrowseNotes
import pnote.stories.UnlockConfidential
import pnote.tools.Banner

fun ProjectorScope.projectBrowseNotes(story: Story<BrowseNotes>) = launch {
    visionLoop@ for (vision in story.subscribe()) {
        when (vision) {
            is BrowseNotes.Importing -> projectImportPassword(vision.substory)
            is BrowseNotes.Unlocking -> projectUnlockConfidential(vision.substory)
            is BrowseNotes.Browsing -> {
                screenLine()
                screenLine("Notes (Confidential)")
                vision.banners.forEachIndexed { i, banner ->
                    banner as Banner.Basic
                    val message = banner.title
                    screenLine("${i + 1}:\u2002$message")
                }
                for (command in generateSequence { promptLine("CONFIDENTIAL", "command") }) {
                    when (command) {
                        "", "done", "quit", "exit", "lock", "bye" -> break@visionLoop
                        else -> screenError("Hmm. I fail to understand the significance of this command: '$command'")
                    }
                }
            }
            BrowseNotes.Finished -> break@visionLoop
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
                vision.setPassword(passwordLine)
            }
            UnlockConfidential.Finished -> break@visionLoop
        }
    }
}
