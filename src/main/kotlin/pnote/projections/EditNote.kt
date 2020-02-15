package pnote.projections

import com.googlecode.lanterna.input.KeyStroke
import com.googlecode.lanterna.input.KeyType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pnote.mainBoxContext
import pnote.projections.sandbox.*

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

class TextEditor {
    private var width: Int = 0
    private var height: Int = 0
    private var cursorColIndex = 0
    private var cursorRowIndex = 0
    private val chars = mutableListOf<MutableList<Char>>()
    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun isCursor(col: Int, row: Int, left: Int, top: Int): Boolean {
        val cursorColIndex = col - left
        val cursorRowIndex = row - top
        return this.cursorColIndex == cursorColIndex && this.cursorRowIndex == cursorRowIndex
    }

    fun getChar(col: Int, row: Int, left: Int, top: Int): Char? {
        val rowIndex = row - top
        return chars.getOrNull(rowIndex)?.let {
            val colIndex = col - left
            it.getOrNull(colIndex)
        }
    }

    fun deletePreviousCharOnLine() {
        if (cursorColIndex > 0) {
            cursorColIndex -= 1
            chars.getOrNull(cursorRowIndex)?.removeAt(cursorColIndex)
        }
    }

    fun insertChar(char: Char) {
        val rowChars =
            if (cursorRowIndex < chars.size) {
                chars[cursorRowIndex]
            } else {
                mutableListOf<Char>().also {
                    chars.add(it)
                    cursorRowIndex = chars.lastIndex
                }
            }
        val charCol =
            if (cursorColIndex < rowChars.size) {
                rowChars.add(cursorColIndex, char)
                cursorColIndex
            } else rowChars.size.also {
                rowChars.add(char)
                cursorColIndex = it
            }
        cursorColIndex = charCol + 1
    }
}

fun BoxContext.editBox(): Box<Void> {
    val surfaceColor = surfaceSwatch.fillColor
    val textColor = surfaceSwatch.strokeColor
    val highlightColor = secondarySwatch.fillColor
    val id = randomId()
    val textEditor = TextEditor()
    return box(
        name = "EditBox",
        render = {
            val bounds = edge.bounds
            if (bounds.contains(col, row)) {
                setColor(surfaceColor, bounds.z)
                textEditor.setSize(bounds.width, bounds.height - 1)
                if (row == bounds.bottom - 1) {
                    if (activeFocusId == id) setGlyph('_', highlightColor, bounds.z)
                } else {
                    textEditor.getChar(col, row, bounds.left, bounds.top)?.let { setGlyph(it, textColor, bounds.z) }
                    if (activeFocusId == id) {
                        if (row == bounds.bottom - 1) setGlyph('_', highlightColor, bounds.z)
                        val isCursor = textEditor.isCursor(col, row, bounds.left, bounds.top)
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
                            if (!char.isISOControl()) {
                                textEditor.insertChar(char)
                                boxScreen.refreshScreen()
                            }
                        }
                        KeyType.Backspace -> {
                            textEditor.deletePreviousCharOnLine()
                            boxScreen.refreshScreen()
                        }
                        else -> Unit
                    }
                }
            }))
        },
        setContent = noContent
    )
}