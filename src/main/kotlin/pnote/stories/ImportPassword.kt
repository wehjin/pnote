package pnote.stories

import com.rubyhuntersky.story.core.matchingStory
import com.rubyhuntersky.story.core.scopes.on
import pnote.scopes.AppScope
import pnote.scopes.PasswordRef
import pnote.stories.ImportPassword.Action.Cancel
import pnote.stories.ImportPassword.Action.SetPassword
import pnote.stories.ImportPassword.UserError
import pnote.stories.ImportPassword.Vision.GetPassword

object ImportPassword {

    sealed class Vision {
        data class GetPassword(val password: String, val check: String, val error: UserError?) : Vision()
        data class FinishedGetPassword(val passwordRef: PasswordRef?) : Vision()
    }

    sealed class Action {
        object Cancel : Action()
        data class SetPassword(val passwordLine: String, val checkLine: String) : Action()
    }

    enum class Subject { Password, Check }

    enum class UserError {
        InvalidPassword,
        MismatchedPasswords,
    }
}

fun AppScope.importPasswordStory() = matchingStory<ImportPassword.Vision>(
    name = "ImportPassword",
    toFirstVision = { GetPassword("", "", null) },
    isLastVision = { vision -> vision is ImportPassword.Vision.FinishedGetPassword },
    updateRules = {
        on<Cancel, ImportPassword.Vision, GetPassword> {
            ImportPassword.Vision.FinishedGetPassword(null)
        }
        on<SetPassword, ImportPassword.Vision, GetPassword> {
            val password = action.passwordLine
            val check = action.checkLine
            when {
                password.isEmpty() -> GetPassword(password, check, UserError.InvalidPassword)
                check != password -> GetPassword(password, check, UserError.MismatchedPasswords)
                else -> ImportPassword.Vision.FinishedGetPassword(
                    importPassword(
                        password
                    )
                )
            }
        }
    }
)
