@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.projections

import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes
import pnote.stories.browseNotesStory
import pnote.tools.*

fun main() {
    val secret = password("abc")
    val app = object : AppScope {
        override val logTag: String = "BrowseNotes2ProjectionTest"
        override val cryptor: Cryptor = memCryptor(secret, null)
        override val noteBag: NoteBag = object : NoteBag {
            override fun createNote(password: Password, note: Note): Long = error("Unused")
            override fun readNote(password: Password, noteId: Long): Note {
                return Note.Basic(
                    title = StringHandle("Hey"),
                    body = StringHandle("There"),
                    noteId = noteId
                )
            }

            override fun updateNote(password: Password, note: Note): Unit = error("Not Here")
            override fun deleteNote(noteId: Long, password: Password): Unit = error("not implemented")
            override fun readBanners(): ReadBannersResult {
                return when (val accessLevel = cryptor.accessLevel) {
                    AccessLevel.Empty -> ReadBannersResult(accessLevel, emptySet())
                    AccessLevel.ConfidentialLocked -> ReadBannersResult(accessLevel, emptySet())
                    is AccessLevel.ConfidentialUnlocked -> ReadBannersResult(
                        accessLevel,
                        (1L..10).map { Banner.Basic(it, StringHandle("Banner$it")) }.toSet()
                    )
                }
            }
        }
    }
    val story = app.browseNotesStory()
    val boxContext = mainBoxContext()
    val projection = boxContext.projectBrowseNotes(story)
    runBlocking {
        projection.join()
        boxContext.boxScreen.close()
    }
}

fun BoxContext.projectBrowseNotes(story: Story<BrowseNotes>): Job = GlobalScope.launch {
    val subBoxContext = SubBoxContext()
    for (browseNotes in story.subscribe()) {
        println("PROJECT: $browseNotes")
        when (browseNotes) {
            is BrowseNotes.Unlocking -> {
                subBoxContext.subProject(browseNotes.substory.name) {
                    SubProjection(browseNotes.substory.name, projectUnlockIdentity(browseNotes.substory))
                }
            }
            is BrowseNotes.Browsing -> {
                subBoxContext.clear()
                val banners = browseNotes.banners.toList()
                if (banners.isEmpty()) {
                    val backFill = fillBox(surfaceSwatch.fillColor)
                    val addButton = textButtonBox(
                        label = "Add Note",
                        onPress = { TODO() }
                    ).maxWidth(10).maxHeight(1)
                    boxScreen.setBox(addButton.before(backFill))
                } else {
                    val bodyBox = messageBox("All Hidden", surfaceSwatch)
                    val sideSwatch = backgroundSwatch
                    val sideOver = columnBox(
                        3 to titleBox("CONFIDENTIAL"),
                        -1 to listRow(
                            itemLabels = banners.map { (it as Banner.Basic).title.toCharSequence().toString() },
                            bodyBox = bodyBox,
                            swatch = sideSwatch,
                            onActivate = { i -> story.offer(browseNotes.viewNote(banners[i].noteId)) }
                        )
                    )
                    val sideUnder = fillBox(sideSwatch.fillColor)
                    val sideBox = sideOver.before(sideUnder)
                    boxScreen.setBox(bodyBox.packLeft(20, sideBox))
                }
            }
            is BrowseNotes.AwaitingDetails -> subBoxContext.subProject(browseNotes.substory.name) {
                projectNoteDetails(browseNotes.substory)
            }
            else -> boxScreen.setBox(messageBox(browseNotes.javaClass.simpleName, backgroundSwatch))
        }
    }
}

private fun BoxContext.listRow(
    itemLabels: List<String>,
    bodyBox: Box<String>,
    swatch: ColorSwatch,
    onActivate: (Int) -> Unit
): Box<*> {
    val listSwatch = ListSwatch(swatch, primaryLightSwatch)
    return listBox(itemLabels, listSwatch) { i, box ->
        box.update(ListMotion.Activate)
        bodyBox.update("${i + 1} Revealed")
        onActivate(i)
    }
}

private fun BoxContext.titleBox(title: String): Box<Void> {
    val swatch = primarySwatch
    return columnBox(
        1 to gapBox(),
        1 to labelBox(title, swatch.strokeColor, Snap.LEFT).padX(2),
        1 to glyphBox('_', swatch.disabledColor)
    ).before(fillBox(swatch.fillColor))
}

