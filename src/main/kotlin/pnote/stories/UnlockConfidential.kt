package pnote.stories

import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.on
import com.rubyhuntersky.story.core.scopes.onAction
import pnote.scopes.AppScope
import pnote.stories.UnlockConfidential.Finished
import pnote.stories.UnlockConfidential.Unlocking
import pnote.stories.UnlockConfidentialAction.Cancel
import pnote.stories.UnlockConfidentialAction.SetPassword
import pnote.tools.AccessLevel.ConfidentialUnlocked
import pnote.tools.Password

fun AppScope.unlockConfidential() = matchingStory<UnlockConfidential>(
    name = "UnlockConfidential",
    isLastVision = { it is Finished },
    toFirstVision = { Unlocking(offer, 0) }
) {
    onAction<Cancel, UnlockConfidential> {
        Finished(true)
    }
    on<SetPassword, UnlockConfidential, Unlocking> {
        cryptor.unlockConfidential(Password(action.passwordLine.toCharArray()))
        val newAccessLevel = cryptor.accessLevel
        if (newAccessLevel is ConfidentialUnlocked) {
            Finished(false)
        } else {
            Unlocking(offer, vision.failCount + 1)
        }
    }
}

sealed class UnlockConfidential(private val offer: ((Any) -> Boolean)? = null) {

    fun cancel() = offer?.invoke(Cancel)

    class Unlocking(
        private val offer: (Any) -> Boolean,
        val failCount: Int
    ) : UnlockConfidential(offer) {
        fun setPassword(passwordLine: String) = offer(SetPassword(passwordLine))
    }

    data class Finished(val wasCancelled: Boolean) : UnlockConfidential()
}


private sealed class UnlockConfidentialAction {
    object Cancel : UnlockConfidentialAction()
    data class SetPassword(val passwordLine: String) : UnlockConfidentialAction()
}
