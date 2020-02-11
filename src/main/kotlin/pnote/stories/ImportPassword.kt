package pnote.stories

import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.on
import pnote.scopes.AppScope
import pnote.stories.ImportConfidentialAction.Cancel
import pnote.stories.ImportConfidentialAction.SetPassword
import pnote.stories.ImportPassword.PasswordEntryError
import pnote.stories.ImportPassword.Vision.FinishedGetPassword
import pnote.stories.ImportPassword.Vision.GetPassword

object ImportPassword {
    sealed class Vision {
        class GetPassword(
            private val offer: (Any) -> Boolean,
            val password: String,
            val check: String,
            val passwordEntryError: PasswordEntryError?
        ) : Vision() {
            fun setPassword(passwordLine: String, checkLine: String) = offer(SetPassword(passwordLine, checkLine))
            fun cancel() = offer(Cancel)
        }

        object FinishedGetPassword : Vision()
    }

    enum class Subject { Password, Check }
    enum class PasswordEntryError { InvalidPassword, MismatchedPasswords }
}

private sealed class ImportConfidentialAction {
    object Cancel : ImportConfidentialAction()
    data class SetPassword(val passwordLine: String, val checkLine: String) : ImportConfidentialAction()
}

fun AppScope.importPasswordStory() = matchingStory<ImportPassword.Vision>(
    name = "ImportPassword",
    toFirstVision = { GetPassword(offer, "", "", null) },
    isLastVision = { vision -> vision is FinishedGetPassword },
    updateRules = {
        on<Cancel, ImportPassword.Vision, GetPassword> { FinishedGetPassword }
        on<SetPassword, ImportPassword.Vision, GetPassword> {
            val password = action.passwordLine
            val check = action.checkLine
            when {
                password.isEmpty() -> GetPassword(offer, password, check, PasswordEntryError.InvalidPassword)
                check != password -> GetPassword(offer, password, check, PasswordEntryError.MismatchedPasswords)
                else -> cryptor.importConfidential(password).let { FinishedGetPassword }
            }
        }
    }
)
