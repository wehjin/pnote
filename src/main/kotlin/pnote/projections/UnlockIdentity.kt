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
                    val contentBox = columnBox(
                        3 to prefixEdit,
                        1 to gapBox(),
                        3 to phraseEdit,
                        1 to gapBox(),
                        1 to gapBox().packRight(10, buttonBox("Continue").maxHeight(1, Snap.RIGHT))
                    )
                    dialogBox("Sol Name", contentBox).before(fillBox(backgroundSwatch.fillColor))
                }
            }
            boxScreen.setBox(box)
        }
    }
}

private fun BoxContext.columnBox(vararg rows: Pair<Int, Box<Void>>): Box<Void> {
    return rows.reversed().fold(
        initial = gapBox(),
        operation = { sum, next ->
            val (height, box) = next
            sum.packTop(height, box)
        }
    )
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

