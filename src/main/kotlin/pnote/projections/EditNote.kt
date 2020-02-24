package pnote.projections

import com.googlecode.lanterna.input.KeyType
import com.rubyhuntersky.story.core.Story
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import pnote.projections.sandbox.ButtonBoxOption.*
import pnote.scopes.AppScope
import pnote.stories.EditNote
import pnote.stories.editNoteStory
import pnote.tools.*
import pnote.tools.security.plain.PlainDocument
import pnote.userDir

fun main() {
    runBlocking {
        val password = password("a")
        val initNote = Note.Basic(
            noteId = 1001,
            plainDoc = PlainDocument("Ho ho ho\nFull of sound and fury, signifying nothing".toCharArray())
        )
        val app = object : AppScope {
            private val commandName = "pnotes"
            private val userName = "edit-note-projection"
            private val userDir = userDir(commandName, userName)
            override val cryptor: Cryptor = memCryptor(password)
            override val noteBag: NoteBag = FileNoteBag(userDir, cryptor)
            override val logTag: String = "$commandName/$userName"
        }
        try {
            app.noteBag.createNote(password, initNote)
        } catch (e: Throwable) {
        }
        val editNoteStory = app.editNoteStory(password, initNote.noteId)
        val boxContext = mainBoxContext()
        val editJob = boxContext.projectEditNote(editNoteStory)
        editJob.join().also { boxContext.boxScreen.close() }
    }
}

fun BoxContext.projectEditNote(story: Story<EditNote>): Job {
    return GlobalScope.launch {
        visionLoop@ for (vision in story.subscribe()) {
            when (vision) {
                EditNote.FinishedEditing -> break@visionLoop
                is EditNote.Editing -> projectEditing(vision, story)
            }
        }
    }
}

private fun BoxContext.projectEditing(vision: EditNote.Editing, story: Story<EditNote>) {
    var contentChange: Pair<List<Char>, List<Char>>? = null
    val contentBox = contentBox(
        vision = vision,
        onChange = { contentChange = it }
    )
    val contentBar = contentBox.maxWidth(60, 0f).pad(6, 1)
    val topBar = topBarBox(
        onBack = { story.offer(vision.cancel()) },
        onSave = {
            val change = contentChange
            story.offer(
                if (change != null) {
                    vision.save(
                        title = String(change.first.toCharArray()),
                        body = String(change.second.toCharArray())
                    )
                } else {
                    vision.cancel()
                }
            )
        }
    )
    val fill = fillBox(backgroundSwatch.fillColor)
    val box = contentBar.packTop(3, topBar).before(fill)
    boxScreen.setBox(box)
}

private fun BoxContext.topBarBox(onBack: () -> Unit, onSave: () -> Unit): Box<Void> {
    val swatch = primaryDarkSwatch
    val title = labelBox("Confidential Note", swatch.strokeColor, Snap.LEFT)
    val styleOptions = setOf(
        EnabledSwatch(swatch),
        FocusedSwatch(primarySwatch),
        PressedSwatch(primaryLightSwatch)
    )
    val saveButton = buttonBox("SAVE", styleOptions + PressReader { onSave() })
    val backButton = buttonBox(
        text = "<-",
        options = styleOptions + SparkReader(Spark.Back) { onBack() } + PressReader { onBack() }
    )
    val content = title.packRight(8, saveButton).packLeft(6, backButton)
    val fill = fillBox(swatch.fillColor)
    return content.before(fill)
}

private fun BoxContext.contentBox(
    vision: EditNote.Editing,
    onChange: (Pair<List<Char>, List<Char>>) -> Unit
): Box<Void> {
    val note = vision.note as Note.Basic

    val plainLines = note.plainDoc.asLines()
    val titleLine = plainLines.firstOrNull()?.toCharSequence()
    val bodyLine = titleLine?.let { plainLines.drop(1).firstOrNull() }?.toCharSequence()
    var nextTitle: List<Char>? = null
    var nextBody: List<Char>? = null

    val titleBox = lineEditBox(
        label = "Title",
        line = titleLine,
        swatch = backgroundSwatch,
        toExtra = { null },
        onChange = {
            nextTitle = it
            onChange(Pair(it, nextBody ?: bodyLine?.toList() ?: emptyList()))
        })
    val titleRow = titleBox.maxHeight(3)

    val bodyBox = editBox(bodyLine) {
        nextBody = it
        onChange(Pair(nextTitle ?: titleLine?.toList() ?: emptyList(), it))
    }
    val bodyRow = bodyBox.packBottom(4, gapBox())
    return bodyRow.packTop(4, titleRow)
}

sealed class ExtraLabel {
    abstract val label: String

    data class Info(override val label: String) : ExtraLabel()
    data class Error(override val label: String) : ExtraLabel()
}

fun BoxContext.lineEditBox(
    label: String,
    line: CharSequence?,
    swatch: ColorSwatch,
    toExtra: () -> ExtraLabel? = { null },
    onChange: (List<Char>) -> Unit
): Box<Void> {
    val id = randomId()
    var lineEditor: LineEditor? = null
    fun initEditor(width: Int): LineEditor = lineEditor
        ?: LineEditor(width, line?.toMutableList() ?: mutableListOf(), onChange).also { lineEditor = it }

    return box(
        name = "LineBox",
        render = {
            val boxBounds = edge.bounds
            val extra = toExtra()
            val frameBounds = extra?.let { boxBounds.insetBottom(1) } ?: boxBounds
            val editBounds = frameBounds.insetXY(1)
            val editor = initEditor(editBounds.width)
            val cursorSwatch = (extra as? ExtraLabel.Error)?.let { errorSwatch } ?: secondarySwatch
            if (activeFocusId == id) {
                focusedEditFrame(
                    label = label,
                    swatch = swatch,
                    focusSwatch = cursorSwatch
                ).render(withEdgeBounds(frameBounds))
            } else {
                unfocusedEditFrame(
                    label = label,
                    labelAtTop = editor.charCount > 0,
                    swatch = swatch,
                    glyphSwatch = (extra as? ExtraLabel.Error)?.let { errorSwatch } ?: swatch
                ).render(withEdgeBounds(frameBounds))
            }
            if (editBounds.contains(col, row)) {
                val editIndex = col - editBounds.left
                if (activeFocusId == id && editor.isCursor(editIndex)) {
                    setColor(cursorSwatch.fillColor, frameBounds.z)
                    editor.getDisplayChar(editIndex)?.let { setGlyph(it, cursorSwatch.strokeColor, frameBounds.z) }
                } else {
                    editor.getDisplayChar(editIndex)?.let { setGlyph(it, swatch.strokeColor, frameBounds.z) }
                }
            }
            if (extra != null) {
                val color = when (extra) {
                    is ExtraLabel.Info -> swatch.disabledColor
                    is ExtraLabel.Error -> if (activeFocusId == id) errorSwatch.fillColor else errorSwatch.mediumColor
                }
                val labelBox = labelBox(extra.label, color, Snap.LEFT).padX(1)
                labelBox.render(withEdgeBounds(boxBounds.confineToBottom()))
            }
            edge.bounds.z
        },
        focus = {
            setFocusable(
                Focusable(
                    id, edge.bounds, FocusRole.Edit, keyReader(id) { stroke ->
                        when (stroke.keyType) {
                            KeyType.Character -> {
                                stroke.character.also { char ->
                                    if (!char.isISOControl()) lineEditor?.insertChar(char).also { boxScreen.refreshScreen() }
                                }
                                true
                            }
                            KeyType.Backspace -> {
                                lineEditor?.deletePreviousChar()?.also { boxScreen.refreshScreen() }
                                true
                            }
                            KeyType.ArrowLeft -> {
                                lineEditor?.moveLeft().also { boxScreen.refreshScreen() }
                                true
                            }
                            KeyType.ArrowRight -> {
                                lineEditor?.moveRight().also { boxScreen.refreshScreen() }
                                true
                            }
                            else -> false
                        }
                    })
            )
        },
        setContent = noContent
    )
}

fun BoxContext.editBox(body: CharSequence?, onChange: (List<Char>) -> Unit): Box<Void> {

    val id = randomId()

    var lateEditor: MemoEditor? = null

    fun initEditor(bounds: BoxBounds): MemoEditor = lateEditor ?: MemoEditor(
        width = bounds.width,
        height = bounds.height,
        initMemo = body?.toMutableList() ?: mutableListOf(),
        onChange = onChange
    ).also { lateEditor = it }

    val swatch = backgroundSwatch
    val cursorSwatch = secondarySwatch
    val label = "Body"
    return box(
        name = "EditBox",
        render = {
            val bounds = edge.bounds
            val editBounds = bounds.insetXY(1)
            val editor = initEditor(editBounds)
            if (activeFocusId == id) {
                focusedEditFrame(label, swatch, cursorSwatch).render(this)
            } else {
                unfocusedEditFrame(label, editor.hasChars, swatch).render(this)
            }
            if (editBounds.contains(col, row)) {
                val leftInset = col - editBounds.left
                val topInset = row - editBounds.top
                if (activeFocusId == id && editor.isCursor(leftInset, topInset)) {
                    setColor(cursorSwatch.fillColor, bounds.z)
                    editor.getChar(leftInset, topInset)?.let { setGlyph(it, cursorSwatch.strokeColor, bounds.z) }
                } else {
                    editor.getChar(leftInset, topInset)?.let { setGlyph(it, swatch.strokeColor, bounds.z) }
                }
            }
            edge.bounds.z
        },
        focus = {
            setFocusable(Focusable(id, edge.bounds, FocusRole.Edit, keyReader(id) { keyStroke ->
                when (keyStroke.keyType) {
                    KeyType.Character -> {
                        val char = keyStroke.character
                        if (!char.isISOControl()) lateEditor?.insertChar(char)?.also { boxScreen.refreshScreen() }
                        true
                    }
                    KeyType.Backspace -> {
                        lateEditor?.deletePreviousCharOnLine()?.also { boxScreen.refreshScreen() }
                        true
                    }
                    KeyType.ArrowUp -> {
                        val moved = lateEditor?.moveUp() ?: false
                        if (moved) boxScreen.refreshScreen()
                        moved
                    }
                    KeyType.ArrowDown -> {
                        val moved = lateEditor?.moveDown() ?: false
                        if (moved) boxScreen.refreshScreen()
                        moved
                    }
                    KeyType.ArrowLeft -> {
                        lateEditor?.moveLeft().also { boxScreen.refreshScreen() }
                        true
                    }
                    KeyType.ArrowRight -> {
                        lateEditor?.moveRight().also { boxScreen.refreshScreen() }
                        true
                    }
                    KeyType.Enter -> {
                        lateEditor?.splitLine()?.also { boxScreen.refreshScreen() }
                        true
                    }
                    else -> false
                }
            }))
        },
        setContent = noContent
    )
}
