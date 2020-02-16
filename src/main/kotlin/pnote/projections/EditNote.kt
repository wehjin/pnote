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
import pnote.projections.sandbox.ButtonBoxOption.*
import pnote.stories.EditNote
import pnote.stories.editNoteStory

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
                is EditNote.Editing -> projectEditing(vision, story)
            }
        }
    })
}

private fun BoxContext.projectEditing(vision: EditNote.Editing, story: Story<EditNote>) {
    val topBar = topBarBox(onBack = { story.offer(vision.cancel()) })
    val contentBox = contentBox(vision).maxWidth(45, 0f).pad(6, 1)
    val fill = fillBox(backgroundSwatch.fillColor)
    val box = contentBox.packTop(3, topBar).before(fill)
    boxScreen.setBox(box)
}

private fun BoxContext.topBarBox(onBack: () -> Unit): Box<Void> {
    val swatch = primaryDarkSwatch
    val title = labelBox("Confidential Note", swatch.strokeColor, Snap.LEFT)
    val styleOptions = setOf(
        EnabledSwatch(swatch),
        FocusedSwatch(primarySwatch),
        PressedSwatch(primaryLightSwatch)
    )
    val saveButton = buttonBox("SAVE", styleOptions + PressReader {})
    val backButton = buttonBox(
        text = "<<",
        options = styleOptions + SparkReader(Spark.Back) { onBack() } + PressReader { onBack() }
    )
    val content = title.packRight(8, saveButton).packLeft(6, backButton)
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
    var lineEditor: LineEditor? = null
    fun initEditor(width: Int): LineEditor {
        return lineEditor ?: LineEditor(width, line.toCharSequence().toMutableList()).also { lineEditor = it }
    }

    return box(
        name = "LineBox",
        render = {
            val bounds = edge.bounds
            val editBounds = bounds.insetXY(1)
            val editor = initEditor(editBounds.width)
            if (activeFocusId == id) {
                focusedEditFrame(label).render(this)
            } else {
                unfocusedEditFrame(label, editor.charCount > 0).render(this)
            }
            if (editBounds.contains(col, row)) {
                val editIndex = col - editBounds.left
                if (activeFocusId == id && editor.isCursor(editIndex)) {
                    setColor(secondarySwatch.fillColor, bounds.z)
                    editor.getDisplayChar(editIndex)?.let { setGlyph(it, secondarySwatch.strokeColor, bounds.z) }
                } else {
                    editor.getDisplayChar(editIndex)?.let { setGlyph(it, primaryDarkSwatch.strokeColor, bounds.z) }
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
    val id = randomId()
    var lateEditor: MemoEditor? = null
    fun initEditor(bounds: BoxBounds): MemoEditor {
        return lateEditor ?: MemoEditor(bounds.width, bounds.height).also { lateEditor = it }
    }

    val textColor = primaryDarkSwatch.strokeColor
    val cursorSwatch = secondarySwatch
    val label = "Body"
    return box(
        name = "EditBox",
        render = {
            val bounds = edge.bounds
            val editBounds = bounds.insetXY(1)
            val editor = initEditor(editBounds)
            if (activeFocusId == id) {
                focusedEditFrame(label).render(this)
            } else {
                unfocusedEditFrame(label, editor.hasChars).render(this)
            }
            if (editBounds.contains(col, row)) {
                val leftInset = col - editBounds.left
                val topInset = row - editBounds.top
                if (activeFocusId == id && editor.isCursor(leftInset, topInset)) {
                    setColor(cursorSwatch.fillColor, bounds.z)
                    editor.getChar(leftInset, topInset)?.let { setGlyph(it, cursorSwatch.strokeColor, bounds.z) }
                } else {
                    editor.getChar(leftInset, topInset)?.let { setGlyph(it, textColor, bounds.z) }
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
