package pnote.projections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.*
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
                    // TODO Switch prefix to simple String
                    val prefix = StringHandle("")
                    val phrase = StringHandle("")

                    fun update(prefixChars: List<Char>?, phraseChars: List<Char>?) {
                        prefixChars?.let { prefix.set(it) }
                        phraseChars?.let { phrase.set(it) }
                        boxScreen.refreshScreen()
                    }

                    dialogBox(
                        "Enter Identity",
                        columnBox(
                            3 to lineEditBox("Prefix", prefix, surfaceSwatch) { update(it, null) },
                            1 to gapBox(),
                            3 to lineEditBox("Secret", phrase, surfaceSwatch) { update(null, it) },
                            1 to gapBox(),
                            1 to dialogActionsBox(
                                actions = listOf(
                                    DialogAction("Cancel"),
                                    DialogAction("Unlock") { isValidSolNamePrefix(prefix.toCharSequence().trim()) }
                                ),
                                onPress = {
                                    when (it) {
                                        0 -> vision.cancel()
                                    }
                                })
                        )
                    ).before(fillBox(backgroundSwatch.fillColor))
                }
            }
            boxScreen.setBox(box)
        }
    }
}

data class DialogAction(
    val label: String,
    val isEnabled: () -> Boolean = { true }
)

private fun BoxContext.dialogActionsBox(actions: List<DialogAction>, onPress: (Int) -> Unit): Box<Void> {
    return actions.foldIndexed(
        initial = gapBox(),
        operation = { i, sum, (label, isEnabled) ->
            val box = textButtonBox(
                label = label,
                isEnabled = { isEnabled() },
                onPress = { onPress(i) }
            )
            sum.packRight(1, gapBox()).packRight(label.length + 2, box)
        }
    )
}

private fun BoxContext.columnBox(vararg rows: Pair<Int, Box<Void>>): Box<Void> {
    return rows.reversed().fold(
        initial = gapBox(),
        operation = { sum, (height, box) -> sum.packTop(height, box) }
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

