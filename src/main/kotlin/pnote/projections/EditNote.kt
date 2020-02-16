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
        val titleRow = lineEditBox("Title").maxHeight(3)
        val contentRow = editBox().packBottom(4, gapBox())
        val infoSide = contentRow.packTop(4, titleRow).maxWidth(30).packTop(4, gapBox())
        val actionSide = messageBox("Actions", primaryDarkSwatch)
        val box = infoSide.packRight(actionSideWidth, actionSide)
        boxScreen.setBox(box)
        ending.receive()
    })
}

fun BoxContext.lineEditBox(label: String): Box<Void> {
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
            val editBounds = bounds.indent(1)
            val editor = lineEditor ?: LineEditor(editWidth).also { lineEditor = it }
            fillBox.render(this)
            if (activeFocusId == id) {
                focusScoreBox.render(withEdgeBounds(bounds.confineToBottom()))
                focusLabelBox.render(withEdgeBounds(bounds.confineToTop().indentLeftRight(1)))
            } else {
                scoreBox.render(withEdgeBounds(bounds.confineToBottom()))
                if (editor.charCount == 0) {
                    labelBox.render(withEdgeBounds(bounds.confineToY(1).indentLeftRight(1)))
                } else {
                    labelBox.render(withEdgeBounds(bounds.confineToTop().indentLeftRight(1)))
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
    val surfaceColor = surfaceSwatch.fillColor
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
