package pnote.stories

import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.UnlockConfidential.Finished
import pnote.stories.UnlockConfidential.Unlocking
import pnote.stories.UnlockConfidentialAction.Cancel
import pnote.stories.UnlockConfidentialAction.SetPassword
import pnote.tools.AccessLevel

fun AppScope.unlockConfidential() = matchingStory<UnlockConfidential>(
    name = "UnlockCurrentLevel",
    isLastVision = { it is Finished },
    toFirstVision = { Unlocking(offer, null) }
) {
    onAction<Cancel, UnlockConfidential> { Finished }
    on<SetPassword, UnlockConfidential, Unlocking> {
        cryptor.unlockConfidential(action.passwordLine)
        val newAccessLevel = cryptor.accessLevel
        if (newAccessLevel == AccessLevel.ConfidentialUnlocked) {
            Finished
        } else {
            Unlocking(offer, action.passwordLine)
        }
    }
}

sealed class UnlockConfidential(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Unlocking(
        private val offer: (Any) -> Boolean,
        val invalidAttempt: String?
    ) : UnlockConfidential(offer) {
        fun setPassword(passwordLine: String) = offer(SetPassword(passwordLine))
    }

    object Finished : UnlockConfidential()
}


private sealed class UnlockConfidentialAction {
    object Cancel : UnlockConfidentialAction()
    data class SetPassword(val passwordLine: String) : UnlockConfidentialAction()
}
