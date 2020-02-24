@file:Suppress("EXPERIMENTAL_API_USAGE")

package pnote.projections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.scopes.AppScope
import pnote.stories.*
import pnote.tools.*
import pnote.tools.security.plain.PlainDocument

fun main() {
    val app = object : AppScope {
        override val logTag: String = "BrowseNotes2ProjectionTest"
        override val cryptor: Cryptor = memCryptor()
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
        val bodySwatch = surfaceSwatch
        when (browseNotes) {
            is BrowseNotes.Unlocking -> {
                subBoxContext.subProject(browseNotes.substory.name) {
                    SubProjection(browseNotes.substory.name, projectUnlockIdentity(browseNotes.substory))
                }
            }
            is BrowseNotes.Browsing -> {
                subBoxContext.clear()
                val notes = browseNotes.notes.toList()
                if (notes.isEmpty()) {
                    val backFill = fillBox(surfaceSwatch.fillColor)
                    val addButton = textButtonBox(
                        label = "Add Note",
                        onPress = { TODO() }
                    ).maxWidth(10).maxHeight(1)
                    boxScreen.setBox(addButton.before(backFill))
                } else {
                    val topBarSwatch = primarySwatch
                    val editButton = textButtonBox(
                        label = "Edit",
                        swatch = topBarSwatch.flip(),
                        isEnabled = { browseNotes.selectedNote != null },
                        onPress = { browseNotes.editNote(browseNotes.selectedNote!!) }
                    )
                    val topBarOverlay = gapBox().packRight(6, editButton.maxHeight(1)).padX(2)
                    val topBarUnderlay = fillBox(topBarSwatch.fillColor)
                    val topBar = topBarOverlay.before(topBarUnderlay)
                    val bodyBox =
                        (browseNotes.selectedNote?.let { noteId ->
                            val note = browseNotes.notes.first { it.noteId == noteId }
                            val title = labelBox(
                                note.plainDoc.titleParagraph?.toCharSequence() ?: "Untitled",
                                bodySwatch.strokeColor, Snap.LEFT
                            ).maxHeight(1, Snap.TOP)
                            val body = labelBox(
                                text = note.plainDoc.bodyParagraph?.toCharSequence() ?: "",
                                textColor = bodySwatch.strokeColor,
                                snap = Snap.LEFT
                            )
                            val overlay = columnBox(
                                1 to title,
                                1 to gapBox(),
                                1 to body
                            )
                            val underlay = fillBox(bodySwatch.fillColor)
                            overlay
                                .pad(6, 1)
                                .before(underlay)

                        } ?: messageBox("Pnotes", bodySwatch))
                            .packTop(3, topBar)

                    val sideSwatch = backgroundSwatch
                    val sideOver = columnBox(
                        3 to titleBox("CONFIDENTIAL"),
                        1 to glyphBox('_', sideSwatch.disabledColor),
                        -1 to listRow(
                            itemLabels = notes.map { it.title },
                            swatch = sideSwatch,
                            onActivate = { i -> browseNotes.viewNote(notes[i].noteId) }
                        )
                    )
                    val sideUnder = fillBox(sideSwatch.fillColor)
                    val sideBox = sideOver.before(sideUnder)
                    boxScreen.setBox(bodyBox.packLeft(20, sideBox))
                }
            }
            is BrowseNotes.Editing -> {
                val subProjectionName = browseNotes.substory.name
                subBoxContext.subProject(subProjectionName) {
                    SubProjection(subProjectionName, projectEditNote(browseNotes.substory))
                }
            }
            else -> boxScreen.setBox(messageBox(browseNotes.javaClass.simpleName, bodySwatch))
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
    val swatch = backgroundSwatch
    return columnBox(
        1 to gapBox(),
        1 to labelBox(title, primaryLightSwatch.fillColor, Snap.LEFT).padX(2),
        1 to gapBox()
    ).before(fillBox(swatch.fillColor))
}

