package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.projections.sandbox.ButtonBoxOption.*
import pnote.scopes.AppScope
import pnote.stories.NoteDetails
import pnote.stories.NoteDetails.*
import pnote.stories.noteDetailsStory
import pnote.tools.*
import pnote.userDir

fun main() {
    val password = password("a")
    val initNote = Note.Basic(
        title = StringHandle("Ho ho ho"),
        body = StringHandle("Full of sound and fury, signifying nothing"),
        noteId = 1001
    )
    val app = object : AppScope {
        override val logTag: String = "note-details-projection-test"
        override val cryptor: Cryptor = memCryptor(password, password)
        override val noteBag: NoteBag = FileNoteBag(userDir("pnotes", logTag), cryptor)
    }
    app.noteBag.updateNote(password, initNote)

    val story = app.noteDetailsStory(password, initNote.noteId, initNote.title)
    val boxContext = mainBoxContext()
    runBlocking { boxContext.projectNoteDetails(story).job.join() }
    boxContext.boxScreen.close()
}

fun BoxContext.projectNoteDetails(story: Story<NoteDetails>): SubProjection =
    SubProjection(story.name, GlobalScope.launch {
        var subProjecting: String? = null
        visionLoop@ for (vision in story.subscribe()) {
            println("${story.name}: $vision")
            when (vision) {
                is FinishedViewing -> break@visionLoop
                is Viewing -> {
                    val leftPadding = 15
                    val descriptionRow = descriptionRow(vision.title.toCharSequence(), leftPadding)
                    val actionsRow = actionsRow(leftPadding, boxScreen, vision, story)
                    val headerRow = fillBox(backgroundSwatch.fillColor)
                    val pageBox = descriptionRow.packTop(3, actionsRow).packTop(10, headerRow)
                    boxScreen.setBox(pageBox).also { subProjecting = null }
                }
                is Editing -> {
                    if (subProjecting != vision.story.name) {
                        projectEditNote(vision.story)
                        subProjecting = vision.story.name
                    }
                }
            }
        }
    })

private fun BoxContext.actionsRow(
    leftPadding: Int,
    boxScreen: BoxScreen,
    vision: Viewing,
    story: Story<NoteDetails>
): Box<Void> {
    val styleOptions = setOf(
        EnabledSwatch(primarySwatch),
        FocusedSwatch(primaryLightSwatch),
        PressedSwatch(primaryLightSwatch)
    )
    val copyButton = buttonBox(
        text = "Copy Text",
        options = styleOptions + PressReader { boxScreen.refreshScreen() }
    )
    val editButton = buttonBox(
        text = "Edit",
        options = styleOptions + PressReader {
            story.offer(vision.edit(vision.title))
            boxScreen.refreshScreen()
        }
    )
    val backButton = buttonBox(
        text = "Back",
        options = styleOptions + PressReader {
            story.offer(vision.cancel)
            boxScreen.refreshScreen()
        }
    )
    val actionsOverlay = gapBox()
        .packLeft(13, copyButton)
        .packLeft(1, gapBox())
        .packLeft(8, editButton)
        .packLeft(1, gapBox())
        .packLeft(8, backButton)
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