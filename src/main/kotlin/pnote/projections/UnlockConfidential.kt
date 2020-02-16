package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pnote.projections.sandbox.*
import pnote.projections.sandbox.ButtonBoxOption.*
import pnote.stories.UnlockConfidential

fun BoxContext.projectUnlockConfidential(story: Story<UnlockConfidential>): Job {
    return GlobalScope.launch {
        for (vision in story.subscribe()) {
            println("${story.name}: $vision")
            when (vision) {
                is UnlockConfidential.Unlocking -> {
                    var password = ""
                    val errorBox = labelBox(
                        text = if (vision.failCount > 0) "Invalid password" else "",
                        textColor = primaryLightSwatch.fillColor,
                        snap = Snap(0f, 0.5f)
                    )
                    val content = columnBox(
                        1, Snap.CENTER,
                        labelBox("Enter Password", surfaceSwatch.strokeColor),
                        gapBox(),
                        inputBox { password = it; errorBox.setContent(""); boxScreen.refreshScreen() }.maxWidth(20),
                        errorBox.maxWidth(20),
                        gapBox(),
                        buttonBox("Submit", setOf(
                            EnabledSwatch(surfaceSwatch),
                            FocusedSwatch(primaryLightSwatch),
                            PressedSwatch(primarySwatch),
                            PressReader { vision.setPassword(password) }
                        ))
                    )
                    val box = content.before(fillBox(surfaceSwatch.fillColor))
                    boxScreen.setBox(box)
                }
                is UnlockConfidential.Finished -> Unit
            }
        }
    }
}
