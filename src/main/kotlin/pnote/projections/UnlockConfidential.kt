package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.ProjectorScope
import pnote.stories.UnlockConfidential


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
