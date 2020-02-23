@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.projections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.BrowseNotes
import pnote.stories.Story2
import pnote.stories.browseNotesStory
import pnote.stories.viewNote
import pnote.tools.*
import pnote.tools.security.plain.PlainDocument

fun main() {
    val secret = password("abc")
    val app = object : AppScope {
        override val logTag: String = "BrowseNotes2ProjectionTest"
        override val cryptor: Cryptor = memCryptor(secret, null)
        override val noteBag: NoteBag = object : NoteBag {
            override fun createNote(password: Password, note: Note): Long = error("Unused")
            override fun readNote(password: Password, noteId: Long): Note {
                return Note.Basic(
                    noteId = noteId,
                    plainDoc = PlainDocument("Hey\nThere".toCharArray())
                )
            }

            override fun updateNote(password: Password, note: Note): Unit = error("Not Here")
            override fun deleteNote(noteId: Long, password: Password): Unit = error("not implemented")
            override fun readNotes(): ReadNotesResult {
                return when (val accessLevel = cryptor.accessLevel) {
                    AccessLevel.Empty -> ReadNotesResult(accessLevel, emptySet())
                    AccessLevel.ConfidentialLocked -> ReadNotesResult(accessLevel, emptySet())
                    is AccessLevel.ConfidentialUnlocked -> ReadNotesResult(
                        accessLevel,
                        (1L..10).map {
                            Note.Basic(it, PlainDocument("Banner$it".toCharArray()))
                        }.toSet()
                    )
                }
            }
        }
    }
    val story = app.browseNotesStory()
    val boxContext = mainBoxContext()
    runBlocking {
        boxContext.projectBrowseNotes(story).join()
        boxContext.boxScreen.close()
    }
}

fun BoxContext.projectBrowseNotes(story: Story2<BrowseNotes>): Job = GlobalScope.launch {
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
                val banners = browseNotes.notes.toList()
                if (banners.isEmpty()) {
                    val backFill = fillBox(surfaceSwatch.fillColor)
                    val addButton = textButtonBox(
                        label = "Add Note",
                        onPress = { TODO() }
                    ).maxWidth(10).maxHeight(1)
                    boxScreen.setBox(addButton.before(backFill))
                } else {
                    val editButton = textButtonBox(
                        label = "Edit",
                        isEnabled = { browseNotes.selectedNote != null },
                        onPress = {}
                    )
                    val topBarOverlay = gapBox().packRight(6, editButton.maxHeight(1)).padX(2)
                    val topBarUnderlay = fillBox(surfaceSwatch.fillColor)
                    val topBar = topBarOverlay.before(topBarUnderlay)
                    val bodyBox =
                        (browseNotes.selectedNote?.let { noteId ->
                            val note = browseNotes.notes.first { it.noteId == noteId }
                            val title = labelBox(
                                note.plainDoc.titleParagraph?.toCharSequence() ?: "Untitled",
                                backgroundSwatch.strokeColor, Snap.LEFT
                            ).maxHeight(1, Snap.TOP)
                            val body = labelBox(
                                text = note.plainDoc.bodyParagraph?.toCharSequence() ?: "",
                                textColor = backgroundSwatch.strokeColor,
                                snap = Snap.LEFT
                            )
                            val overlay = columnBox(
                                1 to title,
                                1 to gapBox(),
                                1 to body
                            )
                            val underlay = fillBox(backgroundSwatch.fillColor)
                            overlay
                                .pad(2, 1)
                                .before(underlay)

                        } ?: messageBox("Pnotes", backgroundSwatch))
                            .packTop(3, topBar)

                    val sideSwatch = primaryDarkSwatch
                    val sideOver = columnBox(
                        3 to titleBox("CONFIDENTIAL"),
                        -1 to listRow(
                            itemLabels = banners.map { it.plainDoc.toCharSequence().toString() },
                            swatch = sideSwatch,
                            onActivate = { i -> browseNotes.viewNote(banners[i].noteId) }
                        )
                    )
                    val sideUnder = fillBox(sideSwatch.fillColor)
                    val sideBox = sideOver.before(sideUnder)
                    boxScreen.setBox(bodyBox.packLeft(20, sideBox))
                }
            }
            else -> boxScreen.setBox(messageBox(browseNotes.javaClass.simpleName, backgroundSwatch))
        }
    }
}

private fun BoxContext.listRow(
    itemLabels: List<String>,
    swatch: ColorSwatch,
    onActivate: (Int) -> Unit
): Box<*> {
    val listSwatch = ListSwatch(swatch, primaryLightSwatch)
    return listBox(itemLabels, listSwatch) { i, box ->
        box.update(ListMotion.Activate)
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

