package pnote.projections

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.App
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.stories.EditNote
import pnote.stories.editNoteStory
import java.lang.Integer.max

fun main() {
    runBlocking {
        val app = App("pnotes", "edit-note-projection")
        val editNoteStory = app.editNoteStory(StringHandle("Hohoho"))
        val boxContext = mainBoxContext()
        val editNoteProjection = boxContext.projectEditNote(editNoteStory)
        editNoteProjection.job.join().also { boxContext.boxScreen.close() }
    }
}

fun BoxContext.projectEditNote(story: Story<EditNote>): SubProjection {
    return SubProjection(story.name, GlobalScope.launch {
        visionLoop@ for (vision in story.subscribe()) {
            when (vision) {
                EditNote.FinishedEditing -> break@visionLoop
                is EditNote.Editing -> projectEditing(vision)
            }
        }
    })
}

private fun BoxContext.projectEditing(vision: EditNote.Editing) {
    val topBar = topBarBox()
    val contentBox = contentBox(vision).maxWidth(45, 0f).pad(2, 1)
    val fill = fillBox(backgroundSwatch.fillColor)
    val box = contentBox.packTop(3, topBar).before(fill)
    boxScreen.setBox(box)
}

private fun BoxContext.topBarBox(): Box<Void> {
    val swatch = primaryDarkSwatch
    val title = labelBox("Confidential Note", swatch.strokeColor, Snap.LEFT)
    val buttonOptions = setOf(
        BoxOption.SwatchEnabled(swatch),
        BoxOption.SwatchFocused(primarySwatch),
        BoxOption.SwatchPressed(primaryLightSwatch)
    )
    val saveButton = buttonBox("SAVE", buttonOptions) {}
    val backButton = buttonBox("<<", buttonOptions) {}
    val content = title.packRight(6, saveButton).packLeft(6, backButton)
    val fill = fillBox(swatch.fillColor)
    return content.before(fill)
}

private fun BoxContext.contentBox(vision: EditNote.Editing): Box<Void> {
    val titleRow = lineEditBox("Title", vision.title).maxHeight(3)
    val contentRow = editBox().packBottom(4, gapBox())
    return contentRow.packTop(4, titleRow)
}

fun BoxContext.lineEditBox(label: String, line: StringHandle): Box<Void> {
    val id = randomId()
    val fillBox = fillBox(primaryDarkSwatch.fillColor)
    val focusScoreBox = glyphBox('_', secondarySwatch.fillColor)
    val focusLabelBox = labelBox(label, secondarySwatch.fillColor, Snap.LEFT)
    val labelBox = labelBox(label, primarySwatch.fillColor, Snap.LEFT)
    val scoreBox = glyphBox('_', primarySwatch.fillColor)
    var lineEditor: LineEditor? = null
    return box(
        name = "LineBox",
        render = {
            val bounds = edge.bounds
            val editWidth = max(0, bounds.width - 2)
            val editBounds = bounds.insetXY(1)
            val editor = lineEditor
                ?: LineEditor(editWidth, line.toCharSequence().toMutableList()).also { lineEditor = it }
            fillBox.render(this)
            if (activeFocusId == id) {
                focusScoreBox.render(withEdgeBounds(bounds.confineToBottom()))
                focusLabelBox.render(withEdgeBounds(bounds.confineToTop().insetX(1)))
            } else {
                scoreBox.render(withEdgeBounds(bounds.confineToBottom()))
                if (editor.charCount == 0) {
                    labelBox.render(withEdgeBounds(bounds.confineToY(1).insetX(1)))
                } else {
                    labelBox.render(withEdgeBounds(bounds.confineToTop().insetX(1)))
                }
            }
            if (editBounds.contains(col, row)) {
                val editIndex = col - editBounds.left
                editor.getDisplayChar(editIndex)?.let { setGlyph(it, primaryDarkSwatch.strokeColor, bounds.z) }
                if (activeFocusId == id) {
                    if (editor.isCursor(editIndex)) setColor(secondarySwatch.fillColor, bounds.z)
                }
            }
        },
        focus = {
            setFocusable(
                Focusable(
                    id, edge.bounds, keyReader(id) { stroke ->
                        when (stroke.keyType) {
                            KeyType.Character -> stroke.character.also { char ->
                                if (!char.isISOControl()) lineEditor?.insertChar(char).also { boxScreen.refreshScreen() }
                            }
                            KeyType.Backspace -> lineEditor?.deletePreviousChar()?.also { boxScreen.refreshScreen() }
                            KeyType.ArrowLeft -> lineEditor?.moveLeft().also { boxScreen.refreshScreen() }
                            KeyType.ArrowRight -> lineEditor?.moveRight().also { boxScreen.refreshScreen() }
                            else -> Unit
                        }
                    })
            )
        },
        setContent = noContent
    )
}

fun BoxContext.editBox(): Box<Void> {
    val surfaceColor = primaryDarkSwatch.fillColor
    val textColor = surfaceSwatch.strokeColor
    val highlightColor = secondarySwatch.fillColor
    val id = randomId()
    var lateEditor: MemoEditor? = null
    fun initEditor(bounds: BoxBounds): MemoEditor {
        return lateEditor ?: MemoEditor(bounds.width, bounds.height).also { lateEditor = it }
    }
    return box(
        name = "EditBox",
        render = {
            val bounds = edge.bounds
            if (bounds.contains(col, row)) {
                setColor(surfaceColor, bounds.z)
                val editor = initEditor(bounds)
                if (row == bounds.bottom - 1) {
                    if (activeFocusId == id) setGlyph('_', highlightColor, bounds.z)
                } else {
                    val leftInset = col - bounds.left
                    val topInset = row - bounds.top
                    editor.getChar(leftInset, topInset)?.let { setGlyph(it, textColor, bounds.z) }
                    if (activeFocusId == id) {
                        if (row == bounds.bottom - 1) setGlyph('_', highlightColor, bounds.z)
                        val isCursor = editor.isCursor(leftInset, topInset)
                        if (isCursor) setCursor(col, row)
                    }
                }
            }
        },
        focus = {
            setFocusable(Focusable(id, edge.bounds, object : KeyReader {
                override val readerId: Long = id
                override val handlesUpDown: Boolean = true
                override fun receiveKey(keyStroke: KeyStroke) {
                    when (keyStroke.keyType) {
                        KeyType.Character -> {
                            val char = keyStroke.character
                            if (!char.isISOControl()) lateEditor?.insertChar(char)?.also { boxScreen.refreshScreen() }
                        }
                        KeyType.Backspace -> lateEditor?.deletePreviousCharOnLine()?.also { boxScreen.refreshScreen() }
                        KeyType.ArrowUp -> lateEditor?.moveUp().also { boxScreen.refreshScreen() }
                        KeyType.ArrowDown -> lateEditor?.moveDown().also { boxScreen.refreshScreen() }
                        KeyType.ArrowLeft -> lateEditor?.moveLeft().also { boxScreen.refreshScreen() }
                        KeyType.ArrowRight -> lateEditor?.moveRight().also { boxScreen.refreshScreen() }
                        KeyType.Enter -> lateEditor?.splitLine()?.also { boxScreen.refreshScreen() }
                        else -> Unit
                    }
                }
            }))
        },
        setContent = noContent
    )
}
