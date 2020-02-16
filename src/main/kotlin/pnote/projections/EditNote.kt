package pnote.projections

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*
import java.lang.Integer.max

fun main() {
    runBlocking {
        val boxContext = mainBoxContext()
        boxContext.projectEditNote().job.join()
        boxContext.boxScreen.close()
    }
}

private const val actionSideWidth = 25

fun BoxContext.projectEditNote(): SubProjection {
    val ending = Channel<Unit>(Channel.RENDEZVOUS)
    return SubProjection("EditNote", GlobalScope.launch {
        val titleRow =
            inputBox {}.packTop(2, labelBox("Title", backgroundSwatch.strokeColor, Snap.TOP_LEFT)).maxHeight(3)
        val contentRow = editBox().packBottom(4, gapBox())
        val infoSide = contentRow.packTop(4, titleRow).maxWidth(30).packTop(4, gapBox())
        val actionSide = messageBox("Actions", primaryDarkSwatch)
        val box = infoSide.packRight(actionSideWidth, actionSide)
        boxScreen.setBox(box)
        ending.receive()
    })
}

class LineEditor(
    private val width: Int,
    private val chars: MutableList<Char> = mutableListOf()
) {
    private var leftCharsIndex: Int = 0
    private var cursorCharsIndex: Int = 0
    private val leftVisibleColumns = 2

    fun isCursor(leftInset: Int): Boolean {
        return (cursorCharsIndex - leftCharsIndex) == leftInset
    }

    fun getDisplayChar(leftInset: Int): Char? {
        return if (leftInset == 0 && leftCharsIndex > 0) {
            '\\'
        } else chars.getOrNull(leftCharsIndex + leftInset)
    }

    fun splitLine(): LineEditor {
        val tailChars = chars.subList(cursorCharsIndex, chars.size).toMutableList()
        repeat(tailChars.size) { chars.removeAt(cursorCharsIndex) }
        leftCharsIndex = 0
        cursorCharsIndex = 0
        return LineEditor(width, tailChars)
    }

    fun deletePreviousChar() {
        if (cursorCharsIndex > 0) {
            cursorCharsIndex -= 1
            chars.removeAt(cursorCharsIndex)
            if (cursorCharsIndex - leftVisibleColumns < leftCharsIndex) {
                leftCharsIndex = max(0, cursorCharsIndex - leftVisibleColumns)
            }
        }
    }

    fun insertChar(char: Char) {
        val oldIndex = cursorCharsIndex
        val newCharIndex =
            if (oldIndex < chars.size) oldIndex.also { chars.add(it, char) }
            else chars.size.also { chars.add(char) }
        cursorCharsIndex = newCharIndex + 1
        if (cursorCharsIndex > (leftCharsIndex + width - 1)) {
            leftCharsIndex = cursorCharsIndex - width + 1
        }
    }
}

class TextEditor(private val width: Int, private val height: Int) {
    private var cursorRowIndex = 0
    private val lineEditors = mutableListOf(LineEditor(width))

    fun isCursor(leftInset: Int, topInset: Int): Boolean {
        return if (topInset != this.cursorRowIndex) false else {
            val lineEditor = lineEditors.getOrNull(topInset)
            lineEditor?.isCursor(leftInset) ?: false
        }
    }

    fun getChar(leftInset: Int, topInset: Int): Char? {
        return lineEditors.getOrNull(topInset)?.getDisplayChar(leftInset)
    }

    fun splitLine() {
        val currentLine = lineEditors.getOrNull(cursorRowIndex)
        val newLineEditor = currentLine?.splitLine() ?: LineEditor(width)
        lineEditors.add(cursorRowIndex + 1, newLineEditor)
        cursorRowIndex++
    }

    fun deletePreviousCharOnLine() {
        lineEditors.getOrNull(cursorRowIndex)?.deletePreviousChar()
    }

    fun insertChar(char: Char) {
        val rowEditor =
            if (cursorRowIndex < lineEditors.size) {
                lineEditors[cursorRowIndex]
            } else {
                LineEditor(width).also {
                    lineEditors.add(it)
                    cursorRowIndex = lineEditors.lastIndex
                }
            }
        rowEditor.insertChar(char)
    }
}

fun BoxContext.editBox(): Box<Void> {
    val surfaceColor = surfaceSwatch.fillColor
    val textColor = surfaceSwatch.strokeColor
    val highlightColor = secondarySwatch.fillColor
    val id = randomId()
    var lateEditor: TextEditor? = null
    fun initEditor(bounds: BoxBounds): TextEditor {
        return lateEditor ?: TextEditor(bounds.width, bounds.height).also { lateEditor = it }
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
                        KeyType.Enter -> lateEditor?.splitLine()?.also { boxScreen.refreshScreen() }
                        else -> Unit
                    }
                }
            }))
        },
        setContent = noContent
    )
}
