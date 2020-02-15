package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.App
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.projections.sandbox.BoxOption.SwatchEnabled
import pnote.projections.sandbox.BoxOption.SwatchPressed
import pnote.stories.NoteDetails
import pnote.stories.NoteDetails.FinishedViewing
import pnote.stories.NoteDetails.Viewing
import pnote.stories.noteDetailsStory
import pnote.tools.password

fun main() {
    val app = App("pnote", "note-details-test")
    val story = app.noteDetailsStory(StringHandle("A Title"), password("hey"), 1)
    lanternaBoxScreen().use { boxScreen ->
        val subProjection = mainBoxContext().projectNoteDetails(story, boxScreen)
        runBlocking { subProjection.job.join() }
    }
}

fun BoxContext.projectNoteDetails(story: Story<NoteDetails>, boxScreen: BoxScreen): SubProjection =
    SubProjection(story.name, GlobalScope.launch {
        visionLoop@ for (vision in story.subscribe()) {
            println("${story.name}: $vision")
            when (vision) {
                is Viewing -> {
                    val leftPadding = 15
                    val descriptionRow = descriptionRow(vision.title.toCharSequence(), leftPadding)
                    val actionsRow = actionsRow(leftPadding, boxScreen) { story.offer(vision.cancel) }
                    val headerRow = fillBox(backgroundSwatch.fillColor)
                    val pageBox = descriptionRow.packTop(3, actionsRow).packTop(10, headerRow)
                    boxScreen.setBox(pageBox)
                }
                is FinishedViewing -> break@visionLoop
            }
        }
    })

private fun BoxContext.actionsRow(leftPadding: Int, boxScreen: BoxScreen, finish: () -> Unit): Box<Void> {
    val buttonOptions = setOf(SwatchEnabled(primarySwatch), SwatchPressed(primaryLightSwatch))
    val copyButton = buttonBox("Copy Text", buttonOptions) {
        println("BUTTON!")
        boxScreen.refreshScreen()
    }
    val editButton = buttonBox("Edit", buttonOptions) {
        finish()
    }
    val actionsOverlay = gapBox()
        .packLeft(13, copyButton)
        .packLeft(1, gapBox())
        .packLeft(8, editButton)
        .packLeft(leftPadding, gapBox())
        .maxHeight(1)
    val actionsUnderlay = fillBox(primaryDarkSwatch.fillColor)
    return actionsOverlay.before(actionsUnderlay)
}

private fun BoxContext.descriptionRow(title: CharSequence, leftPadding: Int): Box<Void> {
    val titleBox = labelBox(title, primarySwatch.strokeColor, Snap.LEFT)
    val contentBox =
        labelBox("This is the content of this note", primarySwatch.strokeColor, Snap.TOP_LEFT)
    val descriptionOverlay = contentBox.packTop(3, titleBox).packLeft(leftPadding, gapBox())
    val descriptionUnderlay = fillBox(primarySwatch.fillColor)
    return descriptionOverlay.before(descriptionUnderlay)
}