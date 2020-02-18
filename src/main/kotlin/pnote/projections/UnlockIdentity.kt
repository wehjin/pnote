package pnote.projections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.Story2
import pnote.stories.UnlockIdentity
import pnote.stories.unlockIdentityStory
import pnote.tools.Cryptor
import pnote.tools.NoteBag
import pnote.tools.memCryptor
import pnote.tools.password

fun main() {
    val app = object : AppScope {
        override val logTag: String = "project-unlock-identity-demo"
        override val noteBag: NoteBag get() = error("not implemented")
        override val cryptor: Cryptor get() = memCryptor
        private val memCryptor = memCryptor(password("abc"), null)
    }
    val story = app.unlockIdentityStory()
    val boxProjector = mainBoxContext()
    runBlocking {
        boxProjector.projectUnlockIdentity(story).join()
    }
}

fun BoxContext.projectUnlockIdentity(story: Story2<UnlockIdentity>): Job {
    return GlobalScope.launch {
        for (vision in story.subscribe()) {
            val box = when (vision) {
                is UnlockIdentity.Done -> messageBox(vision.name, backgroundSwatch)
                is UnlockIdentity.Unlocking -> {
                    val prefixEdit = lineEditBox("Prefix", StringHandle(""), surfaceSwatch) {}
                    val phraseEdit = lineEditBox("Phrase", StringHandle(""), surfaceSwatch) {}
                    val contentBox = gapBox().packTop(3, phraseEdit).packTop(1, gapBox()).packTop(3, prefixEdit)
                    val dialogBox = dialogBox("Sol Name", contentBox)
                    val backgroundBox = fillBox(backgroundSwatch.fillColor)
                    dialogBox.before(backgroundBox)
                }
            }
            boxScreen.setBox(box)
        }
    }
}

private fun BoxContext.dialogBox(title: String, contentBox: Box<*>): Box<Void> {
    val titleRow = dialogTitleBox(title)
    val dialogBehind = fillBox(surfaceSwatch.fillColor)
    val dialogFront = contentBox.pad(2, 1).packTop(1, titleRow).packTop(1, gapBox())
    return dialogFront.before(dialogBehind).maxWidth(40).maxHeight(15)
}

private fun BoxContext.dialogTitleBox(dialogTitle: String): Box<String> {
    return labelBox(dialogTitle, surfaceSwatch.strokeColor, Snap.LEFT).padX(2)
}

