package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.launch
import pnote.scopes.LineProjectorScope
import pnote.stories.UnlockConfidential


fun LineProjectorScope.projectUnlockConfidential(story: Story<UnlockConfidential>) = launch {
    visionLoop@ for (vision in story.subscribe()) {
        when (vision) {
            is UnlockConfidential.Unlocking -> {
                screenLine()
                if (vision.failCount > 0) {
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
